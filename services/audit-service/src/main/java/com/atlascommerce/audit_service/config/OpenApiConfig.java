package com.atlascommerce.audit_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Audit Service API",
                version = "v1",
                description = "Audit event tracking and compliance logging service"
        )
)
public class OpenApiConfig {
}