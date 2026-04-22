package com.circleguard.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final EmailService emailService;
    private final SmsService smsService;
    private final PushService pushService;

    public void dispatch(String userId, String message) {
        log.info("Dispatching multi-channel notifications for user: {}", userId);
        
        CompletableFuture.allOf(
            emailService.sendAsync(userId, message),
            smsService.sendAsync(userId, message),
            pushService.sendAsync(userId, message)
        ).handle((result, ex) -> {
            if (ex != null) {
                log.error("Error during multi-channel dispatch for user {}: {}", userId, ex.getMessage());
            } else {
                log.info("Multi-channel dispatch completed successfully for user: {}", userId);
            }
            return result;
        });
    }
}
