package com.atlascommerce.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.rate-limit.login")
public record LoginRateLimitProperties(
        int maxAttempts,
        int ipMaxAttempts,
        long windowSeconds
) {
    public LoginRateLimitProperties {
        if (maxAttempts <= 0) maxAttempts = 5;
        if (ipMaxAttempts <= 0) ipMaxAttempts = 20;
        if (windowSeconds <= 0) windowSeconds = 300;
    }
}