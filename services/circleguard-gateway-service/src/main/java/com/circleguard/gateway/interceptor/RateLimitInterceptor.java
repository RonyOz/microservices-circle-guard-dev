package com.circleguard.gateway.interceptor;

import com.circleguard.gateway.config.RateLimiterProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.UUID;

/**
 * Rate Limiting pattern — sliding window via Redis sorted set.
 *
 * Each request adds a timestamped entry under key "rl:<ip>".
 * Entries older than the window are pruned before counting.
 * Exceeding the limit returns HTTP 429 with Retry-After header.
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rl:";

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String ip = resolveClientIp(request);
        String key = KEY_PREFIX + ip;
        long now = System.currentTimeMillis();
        long windowMs = (long) properties.getWindowSeconds() * 1000;
        long windowStart = now - windowMs;

        // Remove expired entries outside the sliding window
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Add current request — UUID member prevents score deduplication
        redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);

        // Extend TTL to cover the full window
        redisTemplate.expire(key, Duration.ofSeconds(properties.getWindowSeconds() + 1));

        Long count = redisTemplate.opsForZSet().zCard(key);
        if (count != null && count > properties.getRequestsPerWindow()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(properties.getWindowSeconds()));
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Too Many Requests\",\"retryAfter\":" + properties.getWindowSeconds() + "}"
            );
            return false;
        }
        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
