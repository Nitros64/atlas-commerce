package com.atlascommerce.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SessionProperties.class)
@Configuration
public class SecurityPropertiesConfig {
}