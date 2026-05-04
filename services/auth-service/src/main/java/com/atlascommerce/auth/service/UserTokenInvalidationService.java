package com.atlascommerce.auth.service;

import java.time.Instant;
import java.util.Date;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.atlascommerce.auth.exception.ErrorMessages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserTokenInvalidationService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "user:revoked-after:";

    public void revokeAllForUser(String username) {
        redisTemplate.opsForValue().set(
                PREFIX + username,
                String.valueOf(Instant.now().getEpochSecond())
        );
    }

    public boolean isTokenRevokedForUser(String username, Date issuedAt) {
        if (username == null || username.isBlank() || issuedAt == null) {
            return false;
        }

        try {
            String value = redisTemplate.opsForValue().get(PREFIX + username);

            if (value == null) {
                return false;
            }

            long revokedAfter = Long.parseLong(value);
            long tokenIat = issuedAt.toInstant().getEpochSecond();

            return tokenIat <= revokedAfter;

        } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
            log.error("{}: {}", ErrorMessages.REDIS_UNAVAILABLE, ex.getMessage());
            return false;
        }
    }
}