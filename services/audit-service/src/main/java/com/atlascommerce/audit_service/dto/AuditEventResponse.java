package com.atlascommerce.audit_service.dto;

import com.atlascommerce.audit_service.enums.AuditEventType;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Long userId,
        AuditEventType eventType,
        String action,
        String serviceName,
        String details,
        Instant createdAt
) {
}