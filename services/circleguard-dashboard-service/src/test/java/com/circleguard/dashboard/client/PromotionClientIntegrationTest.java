package com.circleguard.dashboard.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Inter-service integration tests: dashboard-service → promotion-service (HTTP).
 * WireMock stubs promotion-service responses; graceful degradation fallbacks are
 * verified when the upstream is unavailable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PromotionClientIntegrationTest {

    static final WireMockServer wireMock =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("circleguard.promotion-service.url", () -> "http://localhost:" + wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Autowired
    PromotionClient promotionClient;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void reset() {
        wireMock.resetAll();
        circuitBreakerRegistry.circuitBreaker("promotionService").reset();
    }

    @Test
    void whenPromotionServiceReturns200_thenHealthStatsReturned() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/health-status/stats"))
                .willReturn(okJson("{\"healthy\":1200,\"suspect\":30,\"confirmed\":5}")));

        Map<String, Object> stats = promotionClient.getHealthStats();

        assertThat(stats).containsKey("healthy");
        assertThat(stats.get("healthy")).isEqualTo(1200);
    }

    @Test
    void whenPromotionServiceDown_thenFallbackReturnsDegradedResponse() {
        wireMock.stubFor(get(urlEqualTo("/api/v1/health-status/stats"))
                .willReturn(serviceUnavailable()));

        Map<String, Object> result = promotionClient.getHealthStats();

        assertThat(result).containsKey("error");
        assertThat(result.get("error").toString()).contains("unavailable");
    }
}
