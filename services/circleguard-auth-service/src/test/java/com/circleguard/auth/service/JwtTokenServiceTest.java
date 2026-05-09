package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Key;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenServiceTest {

    @Test
    void generateToken_containsPermissionsAndSubject() {
        String secret = "01234567890123456789012345678901"; // 32 chars
        long expiration = 3600000L;
        JwtTokenService service = new JwtTokenService(secret, expiration);

        UUID anon = UUID.randomUUID();

        Authentication auth = new Authentication() {
            @Override
            public java.util.Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(
                        new SimpleGrantedAuthority("PERM_READ"),
                        new SimpleGrantedAuthority("PERM_WRITE")
                );
            }

            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return null; }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException { }
            @Override public String getName() { return "test-auth"; }
        };

        String token = service.generateToken(anon, auth);
        assertNotNull(token);

        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        assertEquals(anon.toString(), claims.getSubject());
        Object permsObj = claims.get("permissions");
        assertNotNull(permsObj);
        assertTrue(permsObj instanceof List);
        List<?> perms = (List<?>) permsObj;
        assertTrue(perms.contains("PERM_READ"));
        assertTrue(perms.contains("PERM_WRITE"));
    }
}
