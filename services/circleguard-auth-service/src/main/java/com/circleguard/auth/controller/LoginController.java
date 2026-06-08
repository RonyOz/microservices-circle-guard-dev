package com.circleguard.auth.controller;

import com.circleguard.auth.service.JwtTokenService;
import com.circleguard.auth.client.IdentityClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final AuthenticationManager authManager;
    private final JwtTokenService jwtService;
    private final IdentityClient identityClient;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;

    public LoginController(AuthenticationManager authManager, JwtTokenService jwtService,
                           IdentityClient identityClient, MeterRegistry meterRegistry) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.identityClient = identityClient;
        this.loginSuccessCounter = Counter.builder("circleguard.auth.login")
                .tag("result", "success")
                .description("Successful login attempts")
                .register(meterRegistry);
        this.loginFailureCounter = Counter.builder("circleguard.auth.login")
                .tag("result", "failure")
                .description("Failed login attempts")
                .register(meterRegistry);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        
        System.out.println("Login attempt for user: " + username + " (pass length: " + (password != null ? password.length() : 0) + ")");

        try {
            // 1. Authenticate (Dual-Chain)
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            System.out.println("Authentication successful for: " + username);

            // 2. Anonymize (Fetch/Create Anonymous ID from Identity Service)
            UUID anonymousId = identityClient.getAnonymousId(username);
            System.out.println("Anonymous ID retrieved: " + anonymousId);

            // 3. Issue Token
            String token = jwtService.generateToken(anonymousId, auth);

            loginSuccessCounter.increment();
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "type", "Bearer",
                    "anonymousId", anonymousId.toString()
            ));
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginFailureCounter.increment();
            System.err.println("Authentication failed for " + username + ": " + e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        } catch (Exception e) {
            System.err.println("Unexpected error during login for " + username + ":");
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/visitor/handoff")
    public ResponseEntity<Map<String, String>> generateVisitorHandoff(Authentication authentication) {
        // The identity comes from the authenticated principal (JWT subject = anonymousId),
        // never from a client-supplied id — otherwise any caller could mint a token for
        // an arbitrary identity (impersonation).
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }

        UUID anonymousId = UUID.fromString(authentication.getName());
        String token = jwtService.generateToken(anonymousId, authentication);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "handoffPayload", "HANDOFF_TOKEN:" + anonymousId + ":" + token
        ));
    }
}
