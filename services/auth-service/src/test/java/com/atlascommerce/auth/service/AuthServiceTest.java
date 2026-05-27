package com.atlascommerce.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.atlascommerce.auth.dto.AuthResponse;
import com.atlascommerce.auth.dto.LoginRequest;
import com.atlascommerce.auth.dto.RegisterRequest;
import com.atlascommerce.auth.entity.Role;
import com.atlascommerce.auth.entity.RoleName;
import com.atlascommerce.auth.entity.UserEntity;
import com.atlascommerce.auth.exception.TooManyLoginAttemptsException;
import com.atlascommerce.auth.repository.RoleRepository;
import com.atlascommerce.auth.repository.UserRepository;
import com.atlascommerce.auth.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private RoleRepository roleRepository;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private LoginRateLimitService loginRateLimitService;

    @InjectMocks
    private AuthService authService;

    private Role userRole() {
        Role role = new Role();
        role.setName(RoleName.ROLE_USER);
        return role;
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("test@atlas.com");
        user.setUsername("test@atlas.com");
        user.setPassword("encoded");
        user.setEnabled(true);
        user.setRoles(Set.of(userRole()));
        return user;
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request =
                new RegisterRequest("test@atlas.com", "123456");

        Role role = userRole();
        UserEntity savedUser = user();

        when(userRepository.existsByEmail("test@atlas.com"))
                .thenReturn(false);

        when(roleRepository.findByName(RoleName.ROLE_USER))
                .thenReturn(Optional.of(role));

        when(passwordEncoder.encode("123456"))
                .thenReturn("encoded");

        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(savedUser);

        when(jwtService.generateAccessToken(savedUser))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(savedUser))
                .thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertEquals("User registered successfully", response.message());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());

        verify(userRepository).save(any(UserEntity.class));
        verify(refreshTokenService)
                .createRefreshToken(savedUser, "refresh-token");
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("test@atlas.com", "123456");

        when(userRepository.existsByEmail("test@atlas.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, 
                () -> authService.register(request));

        verify(userRepository, never()).save(any());
        verify(loginRateLimitService, never()).checkLoginAllowed(anyString(), anyString());
    }

    @Test
    void login_shouldCheckRateLimitAndRegisterFailedAttempt_onBadCredentials() {
        LoginRequest request = new LoginRequest("test@atlas.com", "wrong");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, 
                () -> authService.login(request, "127.0.0.1"));

        verify(loginRateLimitService)
                .checkLoginAllowed("test@atlas.com", "127.0.0.1");

        verify(loginRateLimitService)
                .registerFailedAttempt("test@atlas.com", "127.0.0.1");

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_shouldResetRateLimit_onSuccessfulLogin() {
        LoginRequest request = new LoginRequest("test@atlas.com", "123456");
        UserEntity user = user();

        when(userRepository.findByEmail("test@atlas.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request, "127.0.0.1");

        assertEquals("Login successful", response.message());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());

        // Important verifications
        verify(loginRateLimitService)
                .checkLoginAllowed("test@atlas.com", "127.0.0.1");

        verify(loginRateLimitService)
                .resetEmailIpAttempts("test@atlas.com", "127.0.0.1");

        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        verify(refreshTokenService)
                .createRefreshToken(user, "refresh-token");
    }

    @Test
    void login_shouldThrow_whenRateLimitExceeded() {
        LoginRequest request = new LoginRequest("test@atlas.com", "123456");

        doThrow(new TooManyLoginAttemptsException("Too many attempts"))
                .when(loginRateLimitService)
                .checkLoginAllowed("test@atlas.com", "127.0.0.1");

        assertThrows(TooManyLoginAttemptsException.class, 
                () -> authService.login(request, "127.0.0.1"));

        verify(loginRateLimitService)
                .checkLoginAllowed("test@atlas.com", "127.0.0.1");

        // Should NOT call registerFailedAttempt or proceed with login
        verify(loginRateLimitService, never()).registerFailedAttempt(anyString(), anyString());
        verify(userRepository, never()).findByEmail(anyString());
    }
}