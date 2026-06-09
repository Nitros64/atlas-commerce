package com.atlascommerce.inventory_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Inventory Service API",
                version = "v1",
                description = "Inventory reservation and stock management service"
        )
)
public class OpenApiConfig {
}