package com.atlascommerce.notification_service.dto;

import com.atlascommerce.notification_service.enums.NotificationChannel;
import com.atlascommerce.notification_service.enums.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationChannel channel,
        NotificationStatus status,
        String recipient,
        String subject,
        String message,
        String failureReason,
        Instant createdAt,
        Instant sentAt
) {
}