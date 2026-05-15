package com.atlascommerce.inventory_service.dto;

import java.time.Instant;

public record InventoryItemResponse(
        Long id,
        Long productId,
        String sku,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer minimumStockLevel,
        String warehouseLocation,
        boolean lowStock,
        Instant lastUpdated
) {
}