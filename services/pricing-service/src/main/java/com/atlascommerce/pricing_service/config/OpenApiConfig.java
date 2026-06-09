package com.atlascommerce.pricing_service.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Pricing Service API",
                version = "v1",
                description = "Pricing and price calculation service"
        )
)
public class OpenApiConfig {
}