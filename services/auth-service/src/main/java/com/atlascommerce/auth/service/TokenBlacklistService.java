package com.atlascommerce.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.atlascommerce.auth.exception.ErrorMessages;
import com.atlascommerce.auth.exception.RedisOperationException;
import com.atlascommerce.auth.util.HashUtils;

import io.lettuce.core.RedisConnectionException;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "blacklist:access:";

    public void blacklist(String token, Date expiresAt) {
        if (token == null || token.isBlank() || expiresAt == null) {
            return;
        }

        Instant now = Instant.now();
        Instant exp = expiresAt.toInstant();

        if (!exp.isAfter(now)) {
            return;
        }

        Duration ttl = Duration.between(now, exp);

        try {
            
            redisTemplate.opsForValue().set(buildKey(token), "revoked", ttl);
        
        } catch (RedisConnectionFailureException | QueryTimeoutException | RedisConnectionException ex) {
            log.warn("{}: {}", ErrorMessages.REDIS_UNAVAILABLE, ex.getMessage());
            throw new RedisOperationException(ErrorMessages.REDIS_UNAVAILABLE, ex);
        } catch (Exception ex) {
            log.error(ErrorMessages.REDIS_UNEXPECTED_ERROR, ex);
        }
        
    }
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(token)));
        } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
            log.error("{}: {}", ErrorMessages.REDIS_UNAVAILABLE, ex.getMessage());
            return false;
        }
    }

    private String buildKey(String token) {
        return PREFIX + HashUtils.sha256(token);
    }
}