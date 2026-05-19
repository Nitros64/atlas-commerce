package com.atlascommerce.audit_service.exception;

public class AuditEventNotFoundException extends RuntimeException {

    public AuditEventNotFoundException(Long id) {
        super("Audit event not found with id: " + id);
    }
}