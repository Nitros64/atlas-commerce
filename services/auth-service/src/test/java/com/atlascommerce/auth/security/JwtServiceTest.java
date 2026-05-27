package com.atlascommerce.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                "my-super-secret-key-that-is-at-least-32-chars"
        );

        ReflectionTestUtils.setField(
                jwtService,
                "accessExpirationMs",
                3600000L
        );

        ReflectionTestUtils.setField(
                jwtService,
                "refreshExpirationMs",
                604800000L
        );
    }

    private UserDetails userDetails() {
        return User.builder()
                .username("test@atlas.com")
                .password("password")
                .authorities("ROLE_USER", "ROLE_ADMIN")
                .build();
    }

    @Test
    void generateAccessToken_shouldContainCorrectClaims() {
        String token = jwtService.generateAccessToken(userDetails());

        assertNotNull(token);

        assertEquals(
                "test@atlas.com",
                jwtService.extractUsername(token)
        );

        assertEquals(
                "ACCESS",
                jwtService.extractTokenType(token)
        );

        List<String> roles =
                jwtService.extractRoles(token);

        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void generateRefreshToken_shouldContainRefreshType() {

        String token =
                jwtService.generateRefreshToken(userDetails());

        assertEquals(
                "REFRESH",
                jwtService.extractTokenType(token)
        );
    }

    @Test
    void isValidToken_shouldReturnTrue_forValidToken() {

        String token =
                jwtService.generateAccessToken(userDetails());

        assertTrue(
                jwtService.isValidToken(token, userDetails())
        );
    }

    @Test
    void isValidToken_shouldReturnFalse_forTamperedToken() {

        String token =
                jwtService.generateAccessToken(userDetails());

        token = token + "tampered";

        assertFalse(
                jwtService.isValidToken(token, userDetails())
        );
    }

    @Test
    void generateToken_shouldThrow_whenSecretTooShort() {

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                "short-secret"
        );

        assertThrows(
                IllegalStateException.class,
                () -> jwtService.generateAccessToken(userDetails())
        );
    }
}