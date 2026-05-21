package com.atlascommerce.inventory_service.event;

public record InventoryFailedItemEvent(
        Long productId,
        Integer requestedQuantity,
        String reason
) {
}