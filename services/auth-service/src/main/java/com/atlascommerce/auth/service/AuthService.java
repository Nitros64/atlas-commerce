package com.atlascommerce.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atlascommerce.auth.dto.AuthResponse;
import com.atlascommerce.auth.dto.LoginRequest;
import com.atlascommerce.auth.dto.RegisterRequest;
import com.atlascommerce.auth.entity.Role;
import com.atlascommerce.auth.entity.RoleName;
import com.atlascommerce.auth.entity.UserEntity;
import com.atlascommerce.auth.repository.RoleRepository;
import com.atlascommerce.auth.repository.UserRepository;
import com.atlascommerce.auth.security.JwtService;

import lombok.RequiredArgsConstructor;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    private final RoleRepository roleRepository;

    private final TokenBlacklistService tokenBlacklistService;

    private final LoginRateLimitService loginRateLimitService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        UserEntity user = buildUser(request);
        UserEntity savedUser = userRepository.save(user);

        log.info("User registered successfully with email={}", savedUser.getEmail());

        return generateAuthResponse(savedUser, "User registered successfully");
    }

    private UserEntity buildUser(RegisterRequest request) {

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        return UserEntity.builder()
                .email(request.email())
                .username(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(userRole))
                .enabled(true)
                .build();
    }

    public AuthResponse login(LoginRequest request, String ip) {

        loginRateLimitService.checkLoginAllowed(request.email(), ip);

        try{
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            loginRateLimitService.registerFailedAttempt(request.email(), ip);
            log.warn("Failed login attempt for email: {}", request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        //refreshTokenService.revokeAllUserTokens(user.getId());

        loginRateLimitService.resetEmailIpAttempts(request.email(), ip);

        log.info("User logged in successfully: {}", user.getEmail());        
        return generateAuthResponse(user, "Login successful");
    }

    public AuthResponse refreshToken(String refreshTokenInput) {
        if (!refreshTokenService.isRefreshTokenValid(refreshTokenInput)) {
            throw new IllegalArgumentException("Refresh token invalid/revoked");
        }

        String username = jwtService.extractUsername(refreshTokenInput);
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return generateAuthResponse(user, "Token refreshed successfully");
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (isTokenNullOrBlank(accessToken)) {
            throw new IllegalArgumentException("Access token is required");
        }
        if (isTokenNullOrBlank(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        String username = jwtService.extractUsername(refreshToken);        
       
        refreshTokenService.revokeRefreshToken(refreshToken);
        
        tokenBlacklistService.blacklist(accessToken, jwtService.extractExpiration(accessToken));
        
        log.info("User '{}' logged out successfully", username);
    }

    private AuthResponse generateAuthResponse(UserEntity user, String message) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenService.createRefreshToken(user, refreshToken);
        return AuthResponse.of(message, user, accessToken, refreshToken);
    }

    private boolean isTokenNullOrBlank(String token) {
        return token == null || token.isBlank();
    }
    
}
