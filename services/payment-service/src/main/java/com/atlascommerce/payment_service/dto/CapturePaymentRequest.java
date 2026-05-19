package com.atlascommerce.payment_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CapturePaymentRequest(
        @NotBlank String providerTransactionId
) {
}