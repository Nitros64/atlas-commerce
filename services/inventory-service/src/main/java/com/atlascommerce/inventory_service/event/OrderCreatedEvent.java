package com.atlascommerce.inventory_service.event;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        String currency,
        String createdAt,
        List<OrderItemEvent> items
) {
}