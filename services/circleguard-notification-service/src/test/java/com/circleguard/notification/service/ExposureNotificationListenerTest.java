package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExposureNotificationListenerTest {

    @InjectMocks
    private ExposureNotificationListener listener;

    @Mock
    private NotificationDispatcher dispatcher;

    @Test
    void shouldDispatchNotificationForKnownUserId() {
        String mockEvent = "{\"userId\":\"user-123\",\"newStatus\":\"EXPOSED\"}";

        listener.handleStatusChange(mockEvent);

        verify(dispatcher).dispatch(eq("user-123"), contains("health status has been updated"));
    }

    @Test
    void shouldFallbackToUnknownUserWhenPayloadHasNoUserId() {
        String mockEvent = "{\"newStatus\":\"EXPOSED\"}";

        listener.handleStatusChange(mockEvent);

        verify(dispatcher).dispatch(eq("unknown-user"), contains("health status has been updated"));
    }
}
