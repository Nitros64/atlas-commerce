package com.atlascommerce.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryItemRequest(
        @NotNull Long productId,
        @NotBlank String sku,
        @NotNull @Min(0) Integer availableQuantity,
        @NotNull @Min(0) Integer minimumStockLevel,
        @NotBlank String warehouseLocation
) {
}