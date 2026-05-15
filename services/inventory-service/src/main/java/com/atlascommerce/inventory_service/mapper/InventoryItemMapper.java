package com.atlascommerce.inventory_service.mapper;

import com.atlascommerce.inventory_service.dto.InventoryItemResponse;
import com.atlascommerce.inventory_service.entity.InventoryItem;
import org.springframework.stereotype.Component;

@Component
public class InventoryItemMapper {

    public InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getProductId(),
                item.getSku(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getMinimumStockLevel(),
                item.getWarehouseLocation(),
                item.getAvailableQuantity() <= item.getMinimumStockLevel(),
                item.getLastUpdated()
        );
    }
}