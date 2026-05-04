package com.atlascommerce.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.session")
public record SessionProperties(int maxActive) {

    public SessionProperties {
        if (maxActive <= 0) {
            maxActive = 5;
        }
    }
}