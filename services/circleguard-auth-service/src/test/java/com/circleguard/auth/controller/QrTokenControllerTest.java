package com.circleguard.auth.controller;

import com.circleguard.auth.security.SecurityConfig;
import com.circleguard.auth.service.CustomUserDetailsService;
import com.circleguard.auth.service.QrTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QrTokenController.class)
@Import(SecurityConfig.class)
class QrTokenControllerTest {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrTokenService qrTokenService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    @WithMockUser(username = VALID_UUID)
    void generateToken_returnsQrTokenAndExpiresIn() throws Exception {
        UUID uuid = UUID.fromString(VALID_UUID);
        when(qrTokenService.generateQrToken(uuid)).thenReturn("signed-qr-token-abc");

        mockMvc.perform(get("/api/v1/auth/qr/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrToken").value("signed-qr-token-abc"))
                .andExpect(jsonPath("$.expiresIn").value("60"));
    }

    @Test
    @WithMockUser(username = VALID_UUID)
    void generateToken_delegatesToQrTokenServiceWithAuthenticatedUuid() throws Exception {
        UUID uuid = UUID.fromString(VALID_UUID);
        when(qrTokenService.generateQrToken(uuid)).thenReturn("any-token");

        mockMvc.perform(get("/api/v1/auth/qr/generate"))
                .andExpect(status().isOk());

        verify(qrTokenService).generateQrToken(uuid);
    }
}
