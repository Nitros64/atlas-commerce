package com.atlascommerce.shipping_service.event;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
        Long orderId,
        Long userId,
        String status,
        BigDecimal amount,
        String currency,
        String createdAt
) {
}