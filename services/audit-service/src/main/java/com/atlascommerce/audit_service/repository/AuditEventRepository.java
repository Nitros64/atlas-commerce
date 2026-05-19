package com.atlascommerce.audit_service.repository;

import com.atlascommerce.audit_service.entity.AuditEvent;
import com.atlascommerce.audit_service.enums.AuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByUserId(Long userId);

    List<AuditEvent> findByEventType(AuditEventType eventType);
}