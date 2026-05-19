package com.atlascommerce.audit_service.mapper;

import com.atlascommerce.audit_service.dto.AuditEventResponse;
import com.atlascommerce.audit_service.entity.AuditEvent;
import org.springframework.stereotype.Component;

@Component
public class AuditEventMapper {

    public AuditEventResponse toResponse(AuditEvent auditEvent) {
        return new AuditEventResponse(
                auditEvent.getId(),
                auditEvent.getUserId(),
                auditEvent.getEventType(),
                auditEvent.getAction(),
                auditEvent.getServiceName(),
                auditEvent.getDetails(),
                auditEvent.getCreatedAt()
        );
    }
}