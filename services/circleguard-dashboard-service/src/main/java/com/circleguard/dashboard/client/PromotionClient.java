package com.circleguard.dashboard.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Resilience pattern — Circuit Breaker (Resilience4j).
 * Wraps the synchronous calls to promotion-service. When promotion-service
 * fails repeatedly the breaker opens and requests are short-circuited to the
 * fallback (degraded stats) instead of piling up timeouts. State + metrics are
 * exported to Prometheus via resilience4j-micrometer.
 */
@Component
@Slf4j
public class PromotionClient {

    private static final String CB = "promotionService";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${circleguard.promotion-service.url:http://localhost:8083}")
    private String promotionServiceUrl;

    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = CB, fallbackMethod = "healthStatsFallback")
    public Map<String, Object> getHealthStats() {
        return restTemplate.getForObject(
                promotionServiceUrl + "/api/v1/health-status/stats",
                Map.class
        );
    }

    @SuppressWarnings("unused")
    private Map<String, Object> healthStatsFallback(Throwable t) {
        log.error("Circuit breaker fallback: promotion-service unavailable", t);
        return Map.of("error", "Service unavailable", "timestamp", new Date());
    }

    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = CB, fallbackMethod = "departmentStatsFallback")
    public Map<String, Object> getHealthStatsByDepartment(String department) {
        return restTemplate.getForObject(
                promotionServiceUrl + "/api/v1/health-status/stats/department/" + department,
                Map.class
        );
    }

    @SuppressWarnings("unused")
    private Map<String, Object> departmentStatsFallback(String department, Throwable t) {
        log.error("Circuit breaker fallback: promotion-service department stats unavailable", t);
        return Map.of("error", "Service unavailable", "department", department, "timestamp", new Date());
    }
}
