package com.atlascommerce.gateway_service.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtConfigTest {

    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();

        ReflectionTestUtils.setField(
                jwtConfig,
                "secret",
                "my-super-secret-key-that-is-at-least-32-chars"
        );
    }

    @Test
    void reactiveJwtDecoder_shouldCreateDecoder() {

        ReactiveJwtDecoder decoder =
                jwtConfig.reactiveJwtDecoder();

        assertNotNull(decoder);
    }

   
}