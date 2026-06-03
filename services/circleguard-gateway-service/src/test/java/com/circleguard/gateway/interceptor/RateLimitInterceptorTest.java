package com.circleguard.gateway.interceptor;

import com.circleguard.gateway.config.RateLimiterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    private RateLimiterProperties properties;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new RateLimiterProperties();
        properties.setRequestsPerWindow(3);
        properties.setWindowSeconds(60);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        interceptor = new RateLimitInterceptor(redisTemplate, properties);
    }

    @Test
    void allowsRequest_whenUnderLimit() throws Exception {
        when(zSetOps.zCard(anyString())).thenReturn(2L);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(req, res, new Object());
        assertTrue(result);
        assertNotEquals(429, res.getStatus());
    }

    @Test
    void blocks_whenOverLimit() throws Exception {
        when(zSetOps.zCard(anyString())).thenReturn(10L);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(req, res, new Object());
        assertFalse(result);
        assertEquals(429, res.getStatus());
        assertEquals("60", res.getHeader("Retry-After"));
    }

    @Test
    void usesXForwardedFor_asClientIp() throws Exception {
        when(zSetOps.zCard(anyString())).thenReturn(1L);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(req, res, new Object());
        assertTrue(result);
        verify(zSetOps).add(eq("rl:203.0.113.5"), anyString(), anyDouble());
    }
}
