package com.atlascommerce.order.event;

public record InventoryReservedItemEvent(
        Long productId,
        Integer quantity
) {
}