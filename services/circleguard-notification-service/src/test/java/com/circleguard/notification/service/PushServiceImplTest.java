package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class PushServiceImplTest {

    @Test
    void sendAsync_withMockToken_returnsCompletedFuture() throws Exception {
        PushServiceImpl svc = new PushServiceImpl(WebClient.builder(), "http://localhost:8080", "MOCK_TOKEN");
        CompletableFuture<Void> f = svc.sendAsync("user1", "hello");
        assertNotNull(f);
        f.get();
        assertTrue(f.isDone());
    }
}
