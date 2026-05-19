package com.atlascommerce.audit_service.controller;

import com.atlascommerce.audit_service.dto.AuditEventResponse;
import com.atlascommerce.audit_service.dto.CreateAuditEventRequest;
import com.atlascommerce.audit_service.enums.AuditEventType;
import com.atlascommerce.audit_service.service.AuditEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventService auditEventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public AuditEventResponse create(@Valid @RequestBody CreateAuditEventRequest request) {
        return auditEventService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public AuditEventResponse getById(@PathVariable Long id) {
        return auditEventService.getById(id);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public List<AuditEventResponse> getByUserId(@PathVariable Long userId) {
        return auditEventService.getByUserId(userId);
    }

    @GetMapping("/type/{eventType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public List<AuditEventResponse> getByEventType(@PathVariable AuditEventType eventType) {
        return auditEventService.getByEventType(eventType);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public List<AuditEventResponse> getAll() {
        return auditEventService.getAll();
    }
}