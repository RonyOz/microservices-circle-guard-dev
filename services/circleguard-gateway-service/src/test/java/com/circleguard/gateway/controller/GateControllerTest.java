package com.circleguard.gateway.controller;

import com.circleguard.gateway.interceptor.RateLimitInterceptor;
import com.circleguard.gateway.service.QrValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GateController.class)
public class GateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrValidationService validationService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        // Allow all requests through in controller tests — rate limiting tested separately
        Mockito.when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldReturnValidationResult() throws Exception {
        String token = "mock-token";
        QrValidationService.ValidationResult mockResult =
            new QrValidationService.ValidationResult(true, "GREEN", "Welcome");

        Mockito.when(validationService.validateToken(token)).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/gate/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"mock-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.status").value("GREEN"));
    }

    @Test
    void shouldReturnDenied_whenServiceReturnsRed() throws Exception {
        QrValidationService.ValidationResult mockResult =
            new QrValidationService.ValidationResult(false, "RED", "Access Denied: Health Risk Detected");

        Mockito.when(validationService.validateToken(Mockito.anyString())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/gate/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"risk-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.status").value("RED"))
                .andExpect(jsonPath("$.message").value("Access Denied: Health Risk Detected"));
    }
}
