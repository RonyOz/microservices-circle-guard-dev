package com.circleguard.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-32-chars-long-!!";
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(SECRET);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String buildToken(String subject, List<String> permissions, long expiryMs) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("permissions", permissions)
                .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void validToken_setsAuthenticationWithSubjectAndAuthorities() throws Exception {
        String jwt = buildToken("user-uuid-abc", List.of("PERM_READ", "PERM_WRITE"), 60_000);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-uuid-abc", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("PERM_READ")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("PERM_WRITE")));
        assertNotNull(chain.getRequest(), "Filter chain must be called");
    }

    @Test
    void validTokenWithNullPermissions_setsEmptyAuthorities() throws Exception {
        String jwt = buildToken("user-uuid-xyz", null, 60_000);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-uuid-xyz", auth.getName());
        assertTrue(auth.getAuthorities().isEmpty());
    }

    @Test
    void invalidToken_clearsSecurityContextAndContinuesChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer not.a.valid.jwt");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest(), "Filter chain must still be called after invalid token");
    }

    @Test
    void expiredToken_clearsContextAndContinuesChain() throws Exception {
        String jwt = buildToken("user-uuid-expired", List.of("PERM_READ"), -1_000);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void noAuthorizationHeader_continuesChainWithoutSettingAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void nonBearerAuthorizationHeader_isIgnored() throws Exception {
        String jwt = buildToken("user-uuid-abc", List.of(), 60_000);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic " + jwt);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }
}
