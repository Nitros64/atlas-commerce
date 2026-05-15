package com.atlascommerce.inventory_service.repository;

import com.atlascommerce.inventory_service.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByProductId(Long productId);

    Optional<InventoryItem> findBySku(String sku);

    boolean existsByProductId(Long productId);

    boolean existsBySku(String sku);
}