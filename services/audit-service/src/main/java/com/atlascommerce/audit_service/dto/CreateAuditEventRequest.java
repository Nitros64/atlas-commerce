package com.atlascommerce.audit_service.dto;

import com.atlascommerce.audit_service.enums.AuditEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAuditEventRequest(
        Long userId,

        @NotNull AuditEventType eventType,

        @NotBlank String action,

        @NotBlank String serviceName,

        String details
) {
}