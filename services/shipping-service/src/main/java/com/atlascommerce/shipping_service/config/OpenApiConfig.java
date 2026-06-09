package com.atlascommerce.shipping_service.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Shipping Service API",
                version = "v1",
                description = "Shipping management and shipping event publishing service"
        )
)
public class OpenApiConfig {
}