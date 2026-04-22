package com.circleguard.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    @Async
    public CompletableFuture<Void> sendAsync(String userId, String message) {
        try {
            log.debug("Sending email to user: {}", userId);
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            // In a real app, we'd lookup the user's email from the vault/database
            mailMessage.setTo(userId + "@example.com"); 
            mailMessage.setSubject("CircleGuard Health Alert");
            mailMessage.setText(message);
            
            mailSender.send(mailMessage);
            log.info("Email sent successfully to user: {}", userId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send email to user {}: {}", userId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
