package com.atlascommerce.audit_service.entity;

import com.atlascommerce.audit_service.enums.AuditEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_user_id", columnList = "userId"),
                @Index(name = "idx_audit_event_type", columnList = "eventType")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(nullable = false, length = 200)
    private String action;

    @Column(nullable = false, length = 100)
    private String serviceName;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}