package com.atlascommerce.inventory_service.event;

import java.util.List;

public record InventoryFailedEvent(
        Long orderId,
        Long userId,
        String status,
        String reason,
        String createdAt,
        List<InventoryFailedItemEvent> items
) {
}