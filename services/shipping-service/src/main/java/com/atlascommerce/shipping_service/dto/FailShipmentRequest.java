package com.atlascommerce.shipping_service.dto;

import jakarta.validation.constraints.NotBlank;

public record FailShipmentRequest(
        @NotBlank String failureReason
) {
}