package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SmsServiceImplTest {

    @Test
    void sendAsync_usesMockMode_whenMockSid() throws Exception {
        SmsServiceImpl smsService = new SmsServiceImpl();
        ReflectionTestUtils.setField(smsService, "accountSid", "AC_MOCK_SID");
        ReflectionTestUtils.setField(smsService, "authToken", "MOCK_TOKEN");
        ReflectionTestUtils.setField(smsService, "fromNumber", "+15550000000");

        CompletableFuture<Void> future = smsService.sendAsync("user-1", "test message");

        assertNotNull(future);
        future.get();
        assertTrue(future.isDone());
    }

    @Test
    void sendAsync_returnsFailedFuture_onTwilioError() {
        SmsServiceImpl smsService = new SmsServiceImpl();
        ReflectionTestUtils.setField(smsService, "accountSid", "AC_REAL_SID");
        ReflectionTestUtils.setField(smsService, "authToken", "invalid");

        CompletableFuture<Void> future = smsService.sendAsync("user-1", "message");

        assertTrue(future.isCompletedExceptionally());
    }
}
