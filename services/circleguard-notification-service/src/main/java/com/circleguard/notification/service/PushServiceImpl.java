package com.circleguard.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class PushServiceImpl implements PushService {

    private final WebClient webClient;

    @Value("${push.gotify.url:http://localhost:8080}")
    private String gotifyUrl;

    @Value("${push.gotify.token:MOCK_TOKEN}")
    private String gotifyToken;

    public PushServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(gotifyUrl).build();
    }

    @Override
    @Async
    public CompletableFuture<Void> sendAsync(String userId, String message) {
        if (gotifyToken.equals("MOCK_TOKEN")) {
            log.info("[MOCK PUSH] To: {}, Content: {}", userId, message);
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.debug("Sending push notification to user: {}", userId);
            
            return webClient.post()
                .uri("/message?token=" + gotifyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "title", "CircleGuard Alert",
                    "message", message,
                    "priority", 5
                ))
                .retrieve()
                .toBodilessEntity()
                .toFuture()
                .thenAccept(v -> log.info("Push notification sent successfully to user: {}", userId))
                .exceptionally(ex -> {
                    log.error("Failed to send push notification to user {}: {}", userId, ex.getMessage());
                    return null;
                });
        } catch (Exception e) {
            log.error("Failed to initiate push notification for user {}: {}", userId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
