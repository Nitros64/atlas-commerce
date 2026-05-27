package com.atlascommerce.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class UserTokenInvalidationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserTokenInvalidationService service;

    @Test
    void revokeAllForUser_shouldStoreTimestamp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.revokeAllForUser("test@atlas.com");

        verify(valueOperations).set(
                startsWith("user:revoked-after:test@atlas.com"),
                anyString()
        );
    }

    @Test
    void isTokenRevokedForUser_shouldReturnTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        long now = Instant.now().getEpochSecond();

        when(valueOperations.get("user:revoked-after:test@atlas.com"))
                .thenReturn(String.valueOf(now));

        Date issuedAt = Date.from(Instant.ofEpochSecond(now - 100));

        assertTrue(
                service.isTokenRevokedForUser(
                        "test@atlas.com",
                        issuedAt
                )
        );
    }

    @Test
    void isTokenRevokedForUser_shouldReturnFalse_whenTokenIsNewer() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        long now = Instant.now().getEpochSecond();

        when(valueOperations.get("user:revoked-after:test@atlas.com"))
                .thenReturn(String.valueOf(now));

        Date issuedAt =
                Date.from(Instant.ofEpochSecond(now + 100));

        assertFalse(
                service.isTokenRevokedForUser(
                        "test@atlas.com",
                        issuedAt
                )
        );
    }

    @Test
    void isTokenRevokedForUser_shouldReturnFalse_whenRedisFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertFalse(
                service.isTokenRevokedForUser(
                        "test@atlas.com",
                        new Date()
                )
        );
    }

}