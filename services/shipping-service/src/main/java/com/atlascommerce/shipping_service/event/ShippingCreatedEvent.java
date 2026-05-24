package com.atlascommerce.shipping_service.event;

public record ShippingCreatedEvent(
        Long orderId,
        Long userId,
        String status,
        String trackingNumber,
        String carrier,
        String createdAt
) {
}