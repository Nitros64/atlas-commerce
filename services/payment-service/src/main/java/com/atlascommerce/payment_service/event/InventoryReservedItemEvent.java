package com.atlascommerce.payment_service.event;

public record InventoryReservedItemEvent(
        Long productId,
        Integer quantity
) {
}