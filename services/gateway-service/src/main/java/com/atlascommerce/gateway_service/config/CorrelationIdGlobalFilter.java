package com.atlascommerce.gateway_service.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class CorrelationIdGlobalFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Bean
    public GlobalFilter correlationIdFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(CORRELATION_ID_HEADER);

            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            var request = exchange.getRequest()
                    .mutate()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .build();

            var response = exchange.getResponse();
            response.getHeaders().set(CORRELATION_ID_HEADER, correlationId);

            return chain.filter(exchange.mutate().request(request).build());
        };
    }
}