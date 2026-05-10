package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @InjectMocks
    private EmailServiceImpl emailService;

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @Test
    void sendAsync_sendsEmailCorrectly() throws Exception {
        CompletableFuture<Void> future = emailService.sendAsync("user-42", "Test alert message");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertEquals("user-42@example.com", sent.getTo()[0]);
        assertEquals("CircleGuard Health Alert", sent.getSubject());
        assertEquals("Test alert message", sent.getText());

        assertNotNull(future);
        future.get();
        assertTrue(future.isDone());
    }

    @Test
    void sendAsync_returnsFailedFuture_onMailError() {
        doThrow(new RuntimeException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        CompletableFuture<Void> future = emailService.sendAsync("user-42", "message");

        assertTrue(future.isCompletedExceptionally());
    }
}
