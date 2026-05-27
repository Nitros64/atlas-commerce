package com.atlascommerce.gateway_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter config =
            new CorrelationIdGlobalFilter();

    private final GlobalFilter filter =
            config.correlationIdFilter();

    @Test
    void shouldGenerateCorrelationId_whenHeaderMissing() {

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/test").build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(chain.filter(any()))
                .thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        String correlationId =
                exchange.getResponse()
                        .getHeaders()
                        .getFirst("X-Correlation-ID");

        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());

        verify(chain).filter(any());
    }

    @Test
    void shouldReuseExistingCorrelationId() {

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/test")
                        .header("X-Correlation-ID", "abc-123")
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(chain.filter(any()))
                .thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        String correlationId =
                exchange.getResponse()
                        .getHeaders()
                        .getFirst("X-Correlation-ID");

        assertEquals("abc-123", correlationId);
    }
}