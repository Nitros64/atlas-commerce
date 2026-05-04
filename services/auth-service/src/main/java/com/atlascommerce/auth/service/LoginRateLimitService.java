package com.atlascommerce.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.atlascommerce.auth.config.LoginRateLimitProperties;
import com.atlascommerce.auth.exception.TooManyLoginAttemptsException;
import com.atlascommerce.auth.util.HashUtils;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginRateLimitService {
    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimitProperties properties;
    private static final String EMAIL_IP_PREFIX = "rate-limit:login:email-ip:";
    private static final String IP_PREFIX = "rate-limit:login:ip:";

    public void checkLoginAllowed(String email, String ip) {
        checkKey(emailIpKey(email, ip), properties.maxAttempts());
        checkKey(ipKey(ip), properties.ipMaxAttempts());
    }

    public void registerFailedAttempt(String email, String ip) {
        increment(emailIpKey(email, ip));
        increment(ipKey(ip));
    }

    public void resetEmailIpAttempts(String email, String ip) {
        redisTemplate.delete(emailIpKey(email, ip));
    }

    private void checkKey(String key, int maxAttempts) {
        String value = redisTemplate.opsForValue().get(key);
        int attempts = value == null ? 0 : Integer.parseInt(value);

        if (attempts >= maxAttempts) {
            throw new TooManyLoginAttemptsException("Too many failed login attempts. Try again later.");
        }
    }

    private String emailIpKey(String email, String ip) {
        return EMAIL_IP_PREFIX + HashUtils.sha256(normalize(email) + ":" + normalize(ip));
    }

    private String ipKey(String ip) {
        return IP_PREFIX + HashUtils.sha256(normalize(ip));
    }

    private void increment(String key) {
        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(properties.windowSeconds()));
        }
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase();
    }
}