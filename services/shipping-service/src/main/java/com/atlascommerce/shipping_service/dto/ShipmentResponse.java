package com.atlascommerce.shipping_service.dto;

import com.atlascommerce.shipping_service.enums.ShipmentStatus;
import com.atlascommerce.shipping_service.enums.ShippingProvider;

import java.time.Instant;

public record ShipmentResponse(
        Long id,
        Long orderId,
        Long userId,
        ShipmentStatus status,
        ShippingProvider provider,
        String trackingNumber,
        String recipientName,
        String addressLine,
        String city,
        String country,
        String postalCode,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}