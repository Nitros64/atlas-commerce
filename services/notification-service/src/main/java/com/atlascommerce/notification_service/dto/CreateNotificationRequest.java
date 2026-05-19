package com.atlascommerce.notification_service.dto;

import com.atlascommerce.notification_service.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
        Long userId,

        @NotNull NotificationChannel channel,

        @NotBlank String recipient,

        @NotBlank String subject,

        @NotBlank String message
) {
}