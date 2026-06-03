package com.circleguard.gateway.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.security.Key;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QrValidationServiceTest {

    private static final String SECRET = "abcdefghijklmnopqrstuvwxyz123456";
    private final MeterRegistry registry = new SimpleMeterRegistry();

    private QrValidationService buildService(StringRedisTemplate redis) throws Exception {
        QrValidationService svc = new QrValidationService(redis, registry);
        Field f = QrValidationService.class.getDeclaredField("qrSecret");
        f.setAccessible(true);
        f.set(svc, SECRET);
        return svc;
    }

    private StringRedisTemplate redisReturning(String anonymousId, String status) {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = (ValueOperations<String, String>) Proxy.newProxyInstance(
                ValueOperations.class.getClassLoader(),
                new Class[]{ValueOperations.class},
                (proxy, method, args) -> {
                    if ("get".equals(method.getName()) && args != null && args.length == 1) {
                        String k = (String) args[0];
                        return k.equals("user:status:" + anonymousId) ? status : null;
                    }
                    return null;
                });
        return new StringRedisTemplate() {
            @Override
            public ValueOperations<String, String> opsForValue() { return ops; }
        };
    }

    @Test
    void invalidToken_returnsInvalid() throws Exception {
        QrValidationService svc = buildService(new StringRedisTemplate() {});
        QrValidationService.ValidationResult r = svc.validateToken("badtoken");
        assertFalse(r.valid());
        assertEquals("RED", r.status());
    }

    @Test
    void contagiedStatus_returnsDenied() throws Exception {
        UUID anon = UUID.randomUUID();
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String token = Jwts.builder().setSubject(anon.toString()).signWith(key, SignatureAlgorithm.HS256).compact();

        QrValidationService svc = buildService(redisReturning(anon.toString(), "CONTAGIED"));
        QrValidationService.ValidationResult r = svc.validateToken(token);
        assertFalse(r.valid());
        assertEquals("RED", r.status());
        assertTrue(r.message().contains("Access Denied"));
    }

    @Test
    void potentialStatus_returnsDenied() throws Exception {
        UUID anon = UUID.randomUUID();
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String token = Jwts.builder().setSubject(anon.toString()).signWith(key, SignatureAlgorithm.HS256).compact();

        QrValidationService svc = buildService(redisReturning(anon.toString(), "POTENTIAL"));
        QrValidationService.ValidationResult r = svc.validateToken(token);
        assertFalse(r.valid());
        assertEquals("RED", r.status());
        assertTrue(r.message().contains("Access Denied"));
    }

    @Test
    void greenStatus_returnsValid() throws Exception {
        UUID anon = UUID.randomUUID();
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String token = Jwts.builder().setSubject(anon.toString()).signWith(key, SignatureAlgorithm.HS256).compact();

        QrValidationService svc = buildService(redisReturning(anon.toString(), "GREEN"));
        QrValidationService.ValidationResult r = svc.validateToken(token);
        assertTrue(r.valid());
        assertEquals("GREEN", r.status());
        assertEquals("Welcome to Campus", r.message());
    }
}
