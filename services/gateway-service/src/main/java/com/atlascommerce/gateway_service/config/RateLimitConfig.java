package com.atlascommerce.gateway_service.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        var resolver = XForwardedRemoteAddressResolver.maxTrustedIndex(1);

        return exchange -> {
            var address = resolver.resolve(exchange);

            if (address == null || address.getAddress() == null) {
                return Mono.just("unknown");
            }

            return Mono.just(address.getAddress().getHostAddress());
        };
    }
}