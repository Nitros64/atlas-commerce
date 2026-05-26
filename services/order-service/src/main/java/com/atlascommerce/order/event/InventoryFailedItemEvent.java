package com.atlascommerce.order.event;

public record InventoryFailedItemEvent(
        Long productId,
        Integer requestedQuantity,
        String reason
) {
}