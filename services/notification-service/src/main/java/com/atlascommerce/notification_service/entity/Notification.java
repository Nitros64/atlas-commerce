package com.atlascommerce.notification_service.entity;

import com.atlascommerce.notification_service.enums.NotificationChannel;
import com.atlascommerce.notification_service.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_user_id", columnList = "userId"),
                @Index(name = "idx_notification_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationStatus status;

    @Column(nullable = false, length = 150)
    private String recipient;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    @PrePersist
    void prePersist() {
        if (status == null) status = NotificationStatus.PENDING;
        createdAt = Instant.now();
    }
}