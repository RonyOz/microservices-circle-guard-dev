package com.circleguard.auth.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Inter-service integration tests: auth-service → identity-service (HTTP).
 * WireMock stubs identity-service responses; Resilience4j circuit breaker is
 * exercised via real AOP in the Spring context.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration"
        }
)
class IdentityClientIntegrationTest {

    static final WireMockServer wireMock =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("identity.service.url", () -> "http://localhost:" + wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Autowired
    IdentityClient identityClient;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void reset() {
        wireMock.resetAll();
        circuitBreakerRegistry.circuitBreaker("identityService").reset();
    }

    @Test
    void whenIdentityServiceReturns200_thenAnonymousIdMapped() {
        UUID expected = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/v1/identities/map"))
                .willReturn(okJson("{\"anonymousId\":\"" + expected + "\"}")));

        UUID result = identityClient.getAnonymousId("user@circleguard.edu");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void whenIdentityServiceReturns500_thenCircuitBreakerFallbackThrows() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/identities/map"))
                .willReturn(serverError()));

        assertThatThrownBy(() -> identityClient.getAnonymousId("user@circleguard.edu"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("circuit breaker");
    }

    @Test
    void whenIdentityServiceRepeatedlyFails_thenCircuitBreakerOpens() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/identities/map"))
                .willReturn(serverError()));

        // minimum-number-of-calls=5 with failure-rate-threshold=50 → 5 failures = 100% → OPEN
        for (int i = 0; i < 5; i++) {
            try {
                identityClient.getAnonymousId("user@circleguard.edu");
            } catch (Exception ignored) {
            }
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("identityService");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
