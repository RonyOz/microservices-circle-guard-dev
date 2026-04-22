package com.circleguard.notification.service;

import java.util.concurrent.CompletableFuture;

public interface PushService {
    CompletableFuture<Void> sendAsync(String userId, String message);
}
