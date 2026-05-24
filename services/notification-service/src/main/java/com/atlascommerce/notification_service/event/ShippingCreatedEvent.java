package com.atlascommerce.notification_service.event;

public record ShippingCreatedEvent(
        Long orderId,
        Long userId,
        String status,
        String trackingNumber,
        String carrier,
        String createdAt
) {
}