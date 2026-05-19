package com.atlascommerce.audit_service.service;

import com.atlascommerce.audit_service.dto.AuditEventResponse;
import com.atlascommerce.audit_service.dto.CreateAuditEventRequest;
import com.atlascommerce.audit_service.entity.AuditEvent;
import com.atlascommerce.audit_service.enums.AuditEventType;
import com.atlascommerce.audit_service.exception.AuditEventNotFoundException;
import com.atlascommerce.audit_service.mapper.AuditEventMapper;
import com.atlascommerce.audit_service.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuditEventServiceTest {

    @Mock AuditEventRepository repository;
    @Mock AuditEventMapper mapper;

    @InjectMocks AuditEventService auditEventService;

    private AuditEvent auditEvent;
    private AuditEventResponse response;

    @BeforeEach
    void setUp() {

        auditEvent = AuditEvent.builder()
                .id(1L)
                .userId(1L)
                .eventType(AuditEventType.ORDER_CREATED)
                .action("Order created successfully")
                .serviceName("order-service")
                .details("orderId=1001")
                .createdAt(Instant.now())
                .build();

        response = new AuditEventResponse(
                1L,
                1L,
                AuditEventType.ORDER_CREATED,
                "Order created successfully",
                "order-service",
                "orderId=1001",
                auditEvent.getCreatedAt()
        );
    }

    @Test
    void create_shouldCreateAuditEventSuccessfully() {

        var request = new CreateAuditEventRequest(
                1L,
                AuditEventType.ORDER_CREATED,
                "Order created successfully",
                "order-service",
                "orderId=1001"
        );

        when(repository.save(any(AuditEvent.class))).thenReturn(auditEvent);
        when(mapper.toResponse(any(AuditEvent.class))).thenReturn(response);

        AuditEventResponse result = auditEventService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.eventType()).isEqualTo(AuditEventType.ORDER_CREATED);

        verify(repository).save(any(AuditEvent.class));
    }

    @Test
    void getById_shouldReturnAuditEvent() {

        when(repository.findById(1L)).thenReturn(Optional.of(auditEvent));
        when(mapper.toResponse(auditEvent)).thenReturn(response);

        AuditEventResponse result = auditEventService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void getById_shouldThrow_whenAuditEventDoesNotExist() {

        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditEventService.getById(1L))
                .isInstanceOf(AuditEventNotFoundException.class);
    }

    @Test
    void getByUserId_shouldReturnAuditEvents() {

        when(repository.findByUserId(1L)).thenReturn(List.of(auditEvent));
        when(mapper.toResponse(auditEvent)).thenReturn(response);

        List<AuditEventResponse> result = auditEventService.getByUserId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getByEventType_shouldReturnAuditEvents() {

        when(repository.findByEventType(AuditEventType.ORDER_CREATED))
                .thenReturn(List.of(auditEvent));

        when(mapper.toResponse(auditEvent)).thenReturn(response);

        List<AuditEventResponse> result =
                auditEventService.getByEventType(AuditEventType.ORDER_CREATED);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAll_shouldReturnAuditEvents() {

        when(repository.findAll()).thenReturn(List.of(auditEvent));
        when(mapper.toResponse(auditEvent)).thenReturn(response);

        List<AuditEventResponse> result = auditEventService.getAll();

        assertThat(result).hasSize(1);
    }
}