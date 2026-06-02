package com.circleguard.auth.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/**
 * Resilience pattern — Circuit Breaker (Resilience4j).
 * Protects the auth login flow from a failing identity-service: instead of
 * hanging on timeouts, the breaker opens and the fallback fails fast with a
 * clear error so the caller gets a deterministic 503-style response. State +
 * metrics are exported to Prometheus via resilience4j-micrometer.
 */
@Component
@Slf4j
public class IdentityClient {

    private static final String CB = "identityService";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${identity.service.url:http://localhost:8083}")
    private String identityBaseUrl;

    @CircuitBreaker(name = CB, fallbackMethod = "getAnonymousIdFallback")
    public UUID getAnonymousId(String realIdentity) {
        Map<String, String> request = Map.of("realIdentity", realIdentity);
        Map response = restTemplate.postForObject(identityBaseUrl + "/api/v1/identities/map", request, Map.class);
        return UUID.fromString(response.get("anonymousId").toString());
    }

    @SuppressWarnings("unused")
    private UUID getAnonymousIdFallback(String realIdentity, Throwable t) {
        log.error("Circuit breaker fallback: identity-service unavailable", t);
        throw new IllegalStateException("identity-service unavailable (circuit breaker open)", t);
    }
}
