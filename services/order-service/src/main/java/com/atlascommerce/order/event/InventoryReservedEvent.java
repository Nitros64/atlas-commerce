package com.atlascommerce.order.event;

import java.util.List;

public record InventoryReservedEvent(
        Long orderId,
        Long userId,
        String status,
        String createdAt,
        List<InventoryReservedItemEvent> items
) {
}