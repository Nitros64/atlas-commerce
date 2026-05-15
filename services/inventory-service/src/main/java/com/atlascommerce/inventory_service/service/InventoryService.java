package com.atlascommerce.inventory_service.service;


import com.atlascommerce.inventory_service.dto.CreateInventoryItemRequest;
import com.atlascommerce.inventory_service.dto.InventoryItemResponse;
import com.atlascommerce.inventory_service.dto.StockOperationRequest;
import com.atlascommerce.inventory_service.dto.UpdateInventoryItemRequest;
import com.atlascommerce.inventory_service.entity.InventoryItem;
import com.atlascommerce.inventory_service.exception.DuplicateInventoryItemException;
import com.atlascommerce.inventory_service.exception.InsufficientStockException;
import com.atlascommerce.inventory_service.exception.InventoryItemNotFoundException;
import com.atlascommerce.inventory_service.mapper.InventoryItemMapper;
import com.atlascommerce.inventory_service.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository repository;
    private final InventoryItemMapper mapper;

    @Transactional
    public InventoryItemResponse create(CreateInventoryItemRequest request) {
        if (repository.existsByProductId(request.productId())) {
            throw new DuplicateInventoryItemException("Inventory already exists for productId: " + request.productId());
        }

        if (repository.existsBySku(request.sku())) {
            throw new DuplicateInventoryItemException("Inventory already exists for sku: " + request.sku());
        }

        InventoryItem item = InventoryItem.builder()
                .productId(request.productId())
                .sku(request.sku())
                .availableQuantity(request.availableQuantity())
                .reservedQuantity(0)
                .minimumStockLevel(request.minimumStockLevel())
                .warehouseLocation(request.warehouseLocation())
                .build();

        return mapper.toResponse(repository.save(item));
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getByProductId(Long productId) {
        return mapper.toResponse(findByProductId(productId));
    }

    @Transactional
    public InventoryItemResponse update(Long productId, UpdateInventoryItemRequest request) {
        InventoryItem item = findByProductId(productId);

        item.setSku(request.sku());
        item.setAvailableQuantity(request.availableQuantity());
        item.setReservedQuantity(request.reservedQuantity());
        item.setMinimumStockLevel(request.minimumStockLevel());
        item.setWarehouseLocation(request.warehouseLocation());

        return mapper.toResponse(repository.save(item));
    }

    @Transactional
    public InventoryItemResponse reserve(StockOperationRequest request) {
        InventoryItem item = findByProductId(request.productId());

        if (item.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException(
                    request.productId(),
                    request.quantity(),
                    item.getAvailableQuantity()
            );
        }

        item.setAvailableQuantity(item.getAvailableQuantity() - request.quantity());
        item.setReservedQuantity(item.getReservedQuantity() + request.quantity());

        return mapper.toResponse(repository.save(item));
    }

    @Transactional
    public InventoryItemResponse release(StockOperationRequest request) {
        InventoryItem item = findByProductId(request.productId());

        if (item.getReservedQuantity() < request.quantity()) {
            throw new InsufficientStockException(
                    request.productId(),
                    request.quantity(),
                    item.getReservedQuantity()
            );
        }

        item.setReservedQuantity(item.getReservedQuantity() - request.quantity());
        item.setAvailableQuantity(item.getAvailableQuantity() + request.quantity());

        return mapper.toResponse(repository.save(item));
    }

    @Transactional
    public InventoryItemResponse decrease(StockOperationRequest request) {
        InventoryItem item = findByProductId(request.productId());

        if (item.getReservedQuantity() < request.quantity()) {
            throw new InsufficientStockException(
                    request.productId(),
                    request.quantity(),
                    item.getReservedQuantity()
            );
        }

        item.setReservedQuantity(item.getReservedQuantity() - request.quantity());

        return mapper.toResponse(repository.save(item));
    }

    private InventoryItem findByProductId(Long productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId));
    }
}