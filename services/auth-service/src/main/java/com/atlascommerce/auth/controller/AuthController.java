package com.atlascommerce.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atlascommerce.auth.dto.AuthResponse;
import com.atlascommerce.auth.dto.LoginRequest;
import com.atlascommerce.auth.dto.LogoutRequest;
import com.atlascommerce.auth.dto.RefreshTokenRequest;
import com.atlascommerce.auth.dto.RegisterRequest;
import com.atlascommerce.auth.service.AuthService;
import com.atlascommerce.auth.util.ClientIpUtils;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final ClientIpUtils clientIpUtils;

    public AuthController(AuthService authService, ClientIpUtils clientIpUtils) {
        this.authService = authService;
        this.clientIpUtils = clientIpUtils;
    }

    @PostMapping("/register")
    public  ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user with email={}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = clientIpUtils.getClientIp(httpRequest);
        return ResponseEntity.ok(authService.login(request, ip));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refreshing token for user");
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        log.info("Logging out user");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization bearer token is required");
        }

        String accessToken = authorizationHeader.substring(7).trim();

        authService.logout(accessToken, request.getRefreshToken());

        return ResponseEntity.ok(Map.of(
                "message", "Logout successful",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("auth-service up");
    }
}
