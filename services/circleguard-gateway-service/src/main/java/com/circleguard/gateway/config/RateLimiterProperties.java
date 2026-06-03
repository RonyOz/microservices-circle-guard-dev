package com.circleguard.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * External Configuration Store pattern — rate limiter params injected via
 * K8s ConfigMap (Helm values.yaml → envFrom configMapRef).
 */
@Component
@ConfigurationProperties(prefix = "rate.limit")
public class RateLimiterProperties {

    /** Maximum requests allowed per time window. */
    private int requestsPerWindow = 60;

    /** Sliding window duration in seconds. */
    private int windowSeconds = 60;

    public int getRequestsPerWindow() { return requestsPerWindow; }
    public void setRequestsPerWindow(int requestsPerWindow) { this.requestsPerWindow = requestsPerWindow; }

    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
}
