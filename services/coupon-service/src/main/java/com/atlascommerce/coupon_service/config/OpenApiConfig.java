package com.atlascommerce.coupon_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Coupon Service API",
                version = "v1",
                description = "Coupon validation and discount management service"
        )
)
public class OpenApiConfig {
}