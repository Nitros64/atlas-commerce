package com.atlascommerce.notification_service.dto;

import jakarta.validation.constraints.NotBlank;

public record FailNotificationRequest(
        @NotBlank String failureReason
) {
}