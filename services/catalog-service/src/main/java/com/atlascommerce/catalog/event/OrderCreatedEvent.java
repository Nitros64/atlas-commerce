package com.atlascommerce.catalog.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime createdAt,
        List<OrderItemEvent> items
) {
}