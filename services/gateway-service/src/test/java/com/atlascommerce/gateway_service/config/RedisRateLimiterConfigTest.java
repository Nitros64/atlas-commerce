package com.atlascommerce.gateway_service.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterConfigTest {

    private final RedisRateLimiterConfig config =
            new RedisRateLimiterConfig();

    @Test
    void redisRateLimiter_shouldCreateRedisRateLimiterBean() {
        RedisRateLimiter rateLimiter =
                config.redisRateLimiter();

        assertNotNull(rateLimiter);
    }
}