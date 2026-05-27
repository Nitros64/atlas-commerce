package com.atlascommerce.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.atlascommerce.auth.exception.RedisOperationException;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void blacklist_shouldStoreTokenWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Date expiration =
                Date.from(Instant.now().plusSeconds(3600));

        tokenBlacklistService.blacklist("access-token", expiration);

        verify(valueOperations).set(
                startsWith("blacklist:access:"),
                eq("revoked"),
                any(Duration.class)
        );
    }

    @Test
    void blacklist_shouldIgnoreNullToken() {
        tokenBlacklistService.blacklist(
                null,
                Date.from(Instant.now().plusSeconds(3600))
        );

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void blacklist_shouldIgnoreExpiredToken() {
        Date expiration =
                Date.from(Instant.now().minusSeconds(60));

        tokenBlacklistService.blacklist("token", expiration);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void blacklist_shouldThrowRedisOperationException_whenRedisFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        doThrow(new RedisConnectionFailureException("redis down"))
                .when(valueOperations)
                .set(anyString(), anyString(), any(Duration.class));

        Date expiration =
                Date.from(Instant.now().plusSeconds(3600));

        assertThrows(
                RedisOperationException.class,
                () -> tokenBlacklistService.blacklist("token", expiration)
        );
    }

    @Test
    void isBlacklisted_shouldReturnTrue_whenKeyExists() {
        when(redisTemplate.hasKey(anyString()))
                .thenReturn(true);

        assertTrue(
                tokenBlacklistService.isBlacklisted("access-token")
        );
    }

    @Test
    void isBlacklisted_shouldReturnFalse_whenTokenIsBlank() {
        assertFalse(
                tokenBlacklistService.isBlacklisted(" ")
        );

        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    void isBlacklisted_shouldReturnFalse_whenRedisFails() {
        when(redisTemplate.hasKey(anyString()))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertFalse(
                tokenBlacklistService.isBlacklisted("access-token")
        );
    }
}