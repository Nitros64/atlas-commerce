package com.atlascommerce.payment_service.dto;

import jakarta.validation.constraints.NotBlank;

public record FailPaymentRequest(
        @NotBlank String failureReason
) {
}