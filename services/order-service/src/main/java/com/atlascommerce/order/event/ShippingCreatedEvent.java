package com.atlascommerce.order.event;

public record ShippingCreatedEvent(
        Long orderId,
        Long userId,
        String status,
        String trackingNumber,
        String carrier,
        String createdAt
) {
}