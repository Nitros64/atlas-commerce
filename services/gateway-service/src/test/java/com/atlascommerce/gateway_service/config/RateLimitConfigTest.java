package com.atlascommerce.gateway_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

@ExtendWith(MockitoExtension.class)
class RateLimitConfigTest {

    private final RateLimitConfig config =
            new RateLimitConfig();

    @Test
    void ipKeyResolver_shouldReturnClientIp_fromXForwardedFor() {

        KeyResolver resolver = config.ipKeyResolver();

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/test")
                        .header("X-Forwarded-For", "192.168.1.10")
                        .remoteAddress(
                                new InetSocketAddress("10.0.0.1", 8080)
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        String result = resolver.resolve(exchange).block();

        assertEquals("192.168.1.10", result);
    }

    @Test
    void ipKeyResolver_shouldFallbackToRemoteAddress() {

        KeyResolver resolver = config.ipKeyResolver();

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/test")
                        .remoteAddress(
                                new InetSocketAddress("127.0.0.1", 8080)
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        String result = resolver.resolve(exchange).block();

        assertEquals("127.0.0.1", result);
    }

    @Test
    void ipKeyResolver_shouldReturnUnknown_whenAddressMissing() {

        KeyResolver resolver = config.ipKeyResolver();

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/test")
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        String result = resolver.resolve(exchange).block();

        assertEquals("unknown", result);
    }
}