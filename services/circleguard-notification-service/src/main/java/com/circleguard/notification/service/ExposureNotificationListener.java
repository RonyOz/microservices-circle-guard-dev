package com.circleguard.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExposureNotificationListener {

    private final NotificationDispatcher dispatcher;

    /**
     * Listens for status changes and exposure events.
     */
    @KafkaListener(topics = "promotion.status.changed", groupId = "notification-group")
    public void handleStatusChange(String eventJson) {
        log.info("Received health status change event: {}", eventJson);
        // Extract anonymousId from eventJson (assuming format like {"userId": "...", ...})
        // Simple extraction for now
        String userId = extractUserId(eventJson);
        dispatcher.dispatch(userId, "Attention: Your health status has been updated in CircleGuard. Please check the app for instructions.");
    }

    private String extractUserId(String json) {
        // Simple regex or JSON parser. For now, just a mock extraction
        if (json.contains("\"userId\":\"")) {
            int start = json.indexOf("\"userId\":\"") + 10;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
        return "unknown-user";
    }
}
