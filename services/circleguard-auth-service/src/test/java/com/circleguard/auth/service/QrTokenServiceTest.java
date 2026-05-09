package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QrTokenServiceTest {

    @Test
    void generateQrToken_setsSubjectToAnonymousId() {
        String secret = "abcdefghijklmnopqrstuvwxyz123456"; // 32+ chars
        long expiration = 60000L;
        QrTokenService svc = new QrTokenService(secret, expiration);
        UUID anon = UUID.randomUUID();

        String token = svc.generateQrToken(anon);
        assertNotNull(token);

        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        assertEquals(anon.toString(), claims.getSubject());
    }
}
