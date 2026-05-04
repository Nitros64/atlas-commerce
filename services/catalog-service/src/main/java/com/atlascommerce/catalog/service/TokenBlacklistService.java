package com.atlascommerce.catalog.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.atlascommerce.catalog.util.HashUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "blacklist:access:";

    private String buildKey(String token) {
        return PREFIX + HashUtils.sha256(token);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(buildKey(token))
        );
    }
}