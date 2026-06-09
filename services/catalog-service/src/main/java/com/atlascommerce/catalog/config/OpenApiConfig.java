package com.atlascommerce.catalog.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Catalog Service API",
                version = "v1",
                description = "Product catalog management service"
        )
)
public class OpenApiConfig {
}