package com.atlascommerce.inventory_service.controller;


import com.atlascommerce.inventory_service.dto.CreateInventoryItemRequest;
import com.atlascommerce.inventory_service.dto.InventoryItemResponse;
import com.atlascommerce.inventory_service.dto.StockOperationRequest;
import com.atlascommerce.inventory_service.dto.UpdateInventoryItemRequest;
import com.atlascommerce.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'USER')")
    public InventoryItemResponse create(@Valid @RequestBody CreateInventoryItemRequest request) {
        return inventoryService.create(request);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'SYSTEM', 'USER')")
    public InventoryItemResponse getByProductId(@PathVariable Long productId) {
        return inventoryService.getByProductId(productId);
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public InventoryItemResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateInventoryItemRequest request
    ) {
        return inventoryService.update(productId, request);
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'SYSTEM', 'USER')")
    public InventoryItemResponse reserve(@Valid @RequestBody StockOperationRequest request) {
        return inventoryService.reserve(request);
    }

    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'SYSTEM', 'USER')")
    public InventoryItemResponse release(@Valid @RequestBody StockOperationRequest request) {
        return inventoryService.release(request);
    }

    @PostMapping("/decrease")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'SYSTEM', 'USER')")
    public InventoryItemResponse decrease(@Valid @RequestBody StockOperationRequest request) {
        return inventoryService.decrease(request);
    }
}