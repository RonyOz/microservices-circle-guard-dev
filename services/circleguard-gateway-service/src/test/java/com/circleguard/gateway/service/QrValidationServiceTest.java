package com.circleguard.gateway.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.security.Key;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QrValidationServiceTest {

    @Test
    void validateToken_invalidToken_returnsInvalid() throws Exception {
        String secret = "abcdefghijklmnopqrstuvwxyz123456";
        StringRedisTemplate redis = new StringRedisTemplate(){};
        QrValidationService svc = new QrValidationService(redis);
        Field f = svc.getClass().getDeclaredField("qrSecret");
        f.setAccessible(true);
        f.set(svc, secret);

        QrValidationService.ValidationResult r = svc.validateToken("badtoken");
        assertFalse(r.valid());
        assertEquals("RED", r.status());
    }

    @Test
    void validateToken_withContagiedStatus_returnsDenied() throws Exception {
        String secret = "abcdefghijklmnopqrstuvwxyz123456";
        UUID anon = UUID.randomUUID();
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder().setSubject(anon.toString()).signWith(key, SignatureAlgorithm.HS256).compact();

        @SuppressWarnings("unchecked")
        ValueOperations<String,String> ops = (ValueOperations<String,String>) Proxy.newProxyInstance(
                ValueOperations.class.getClassLoader(),
                new Class[] { ValueOperations.class },
                (proxy, method, args) -> {
                    if ("get".equals(method.getName()) && args != null && args.length == 1) {
                        String k = (String) args[0];
                        if (k.equals("user:status:" + anon.toString())) return "CONTAGIED";
                        return null;
                    }
                    return null;
                }
        );

        StringRedisTemplate redis = new StringRedisTemplate() {
            @Override
            public ValueOperations<String, String> opsForValue() {
                return ops;
            }
        };

        QrValidationService svc = new QrValidationService(redis);
        Field f = svc.getClass().getDeclaredField("qrSecret");
        f.setAccessible(true);
        f.set(svc, secret);

        QrValidationService.ValidationResult r = svc.validateToken(token);
        assertFalse(r.valid());
        assertEquals("RED", r.status());
        assertTrue(r.message().contains("Access Denied"));
    }

    @Test
    void validateToken_withPotentialStatus_returnsDenied() throws Exception {
        String secret = "abcdefghijklmnopqrstuvwxyz123456";
        UUID anon = UUID.randomUUID();
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder().setSubject(anon.toString()).signWith(key, SignatureAlgorithm.HS256).compact();

        @SuppressWarnings("unchecked")
        ValueOperations<String,String> ops = (ValueOperations<String,String>) Proxy.newProxyInstance(
                ValueOperations.class.getClassLoader(),
                new Class[] { ValueOperations.class },
                (proxy, method, args) -> {
                    if ("get".equals(method.getName()) && args != null && args.length == 1) {
                        String k = (String) args[0];
                        if (k.equals("user:status:" + anon.toString())) return "POTENTIAL";
                        return null;
                    }
                    return null;
                }
        );

        StringRedisTemplate redis = new StringRedisTemplate() {
            @Override
            public ValueOperations<String, String> opsForValue() {
                return ops;
            }
        };

        QrValidationService svc = new QrValidationService(redis);
        Field f = svc.getClass().getDeclaredField("qrSecret");
        f.setAccessible(true);
        f.set(svc, secret);

        QrValidationService.ValidationResult r = svc.validateToken(token);
        assertFalse(r.valid());
        assertEquals("RED", r.status());
        assertTrue(r.message().contains("Access Denied"));
    }

    @Test
    void validateToken_withGreenStatus_returnsValid() throws Exception {
        String secret = "abcdefghijklmnopqrstuvwxyz123456";
        UUID anon = UUID.randomUUID();
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder().setSubject(anon.toString()).signWith(key, SignatureAlgorithm.HS256).compact();

        @SuppressWarnings("unchecked")
        ValueOperations<String,String> ops = (ValueOperations<String,String>) Proxy.newProxyInstance(
                ValueOperations.class.getClassLoader(),
                new Class[] { ValueOperations.class },
                (proxy, method, args) -> {
                    if ("get".equals(method.getName()) && args != null && args.length == 1) {
                        String k = (String) args[0];
                        if (k.equals("user:status:" + anon.toString())) return "GREEN";
                        return null;
                    }
                    return null;
                }
        );

        StringRedisTemplate redis = new StringRedisTemplate() {
            @Override
            public ValueOperations<String, String> opsForValue() {
                return ops;
            }
        };

        QrValidationService svc = new QrValidationService(redis);
        Field f = svc.getClass().getDeclaredField("qrSecret");
        f.setAccessible(true);
        f.set(svc, secret);

        QrValidationService.ValidationResult r = svc.validateToken(token);
        assertTrue(r.valid());
        assertEquals("GREEN", r.status());
        assertEquals("Welcome to Campus", r.message());
    }
}
