package com.atlascommerce.inventory_service.event;

public record InventoryReservedItemEvent(
        Long productId,
        Integer quantity
) {
}