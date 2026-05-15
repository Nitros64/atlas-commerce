package com.atlascommerce.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockOperationRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {
}