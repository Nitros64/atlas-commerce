package com.atlascommerce.cart_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Cart Service API",
                version = "v1",
                description = "Shopping cart management service"
        )
)
public class OpenApiConfig {
}