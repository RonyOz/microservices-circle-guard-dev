package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @InjectMocks
    private NotificationDispatcher dispatcher;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private PushService pushService;

    @Test
    void shouldDispatchToAllChannelsConcurrently() throws Exception {
        when(emailService.sendAsync(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(smsService.sendAsync(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(pushService.sendAsync(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        dispatcher.dispatch("user-123", "Your health status has changed.");

        verify(emailService).sendAsync(eq("user-123"), anyString());
        verify(smsService).sendAsync(eq("user-123"), anyString());
        verify(pushService).sendAsync(eq("user-123"), anyString());
    }
}
