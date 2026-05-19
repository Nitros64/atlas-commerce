package com.atlascommerce.audit_service.service;

import com.atlascommerce.audit_service.dto.AuditEventResponse;
import com.atlascommerce.audit_service.dto.CreateAuditEventRequest;
import com.atlascommerce.audit_service.entity.AuditEvent;
import com.atlascommerce.audit_service.enums.AuditEventType;
import com.atlascommerce.audit_service.exception.AuditEventNotFoundException;
import com.atlascommerce.audit_service.mapper.AuditEventMapper;
import com.atlascommerce.audit_service.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository repository;
    private final AuditEventMapper mapper;

    @Transactional
    public AuditEventResponse create(CreateAuditEventRequest request) {
        AuditEvent event = AuditEvent.builder()
                .userId(request.userId())
                .eventType(request.eventType())
                .action(request.action())
                .serviceName(request.serviceName())
                .details(request.details())
                .build();

        return mapper.toResponse(repository.save(event));
    }

    @Transactional(readOnly = true)
    public AuditEventResponse getById(Long id) {
        return mapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getByUserId(Long userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getByEventType(AuditEventType eventType) {
        return repository.findByEventType(eventType)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private AuditEvent findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AuditEventNotFoundException(id));
    }
}