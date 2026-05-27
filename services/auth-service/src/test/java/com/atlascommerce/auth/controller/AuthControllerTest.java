package com.atlascommerce.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.atlascommerce.auth.dto.AuthResponse;
import com.atlascommerce.auth.dto.LoginRequest;
import com.atlascommerce.auth.dto.LogoutRequest;
import com.atlascommerce.auth.dto.RegisterRequest;
import com.atlascommerce.auth.exception.GlobalExceptionHandler;
import com.atlascommerce.auth.service.AuthService;
import com.atlascommerce.auth.util.ClientIpUtils;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private ClientIpUtils clientIpUtils;

    private ObjectMapper objectMapper;

    @BeforeEach
        void setUp() {
        objectMapper = new ObjectMapper();

        AuthController controller =
                new AuthController(authService, clientIpUtils);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        }

    @Test
    void ping_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/auth/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("auth-service up"));
    }

    @Test
    void register_shouldReturnCreated() throws Exception {
        RegisterRequest request =
                new RegisterRequest("test@atlas.com", "123456");

        AuthResponse response =
                new AuthResponse(
                        "User registered successfully",
                        "test@atlas.com",
                        Set.of("ROLE_USER"),
                        "access-token",
                        "refresh-token"
                );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_shouldReturnOk() throws Exception {
        LoginRequest request =
                new LoginRequest("test@atlas.com", "123456");

        AuthResponse response =
                new AuthResponse(
                        "Login successful",
                        "test@atlas.com",
                        Set.of("ROLE_USER"),
                        "access-token",
                        "refresh-token"
                );

        when(clientIpUtils.getClientIp(any()))
                .thenReturn("127.0.0.1");

        when(authService.login(any(LoginRequest.class), eq("127.0.0.1")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void logout_shouldReturnOk() throws Exception {
        LogoutRequest request =
                new LogoutRequest("refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService).logout("access-token", "refresh-token");
    }

    @Test
    void logout_shouldThrow_whenAuthorizationHeaderIsInvalid() throws Exception {
        LogoutRequest request =
                new LogoutRequest("refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Invalid access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}