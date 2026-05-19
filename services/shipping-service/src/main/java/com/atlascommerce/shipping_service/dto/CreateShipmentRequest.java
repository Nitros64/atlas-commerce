package com.atlascommerce.shipping_service.dto;

import com.atlascommerce.shipping_service.enums.ShippingProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateShipmentRequest(
        @NotNull Long orderId,
        @NotNull Long userId,
        @NotNull ShippingProvider provider,

        @NotBlank String recipientName,
        @NotBlank String addressLine,
        @NotBlank String city,
        @NotBlank String country,
        @NotBlank String postalCode
) {
}