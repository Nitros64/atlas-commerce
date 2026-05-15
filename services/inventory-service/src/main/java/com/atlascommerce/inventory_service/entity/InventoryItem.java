package com.atlascommerce.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "inventory_items",
        indexes = {
                @Index(name = "idx_inventory_product_id", columnList = "productId"),
                @Index(name = "idx_inventory_sku", columnList = "sku")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false)
    private Integer availableQuantity;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Column(nullable = false)
    private Integer minimumStockLevel;

    @Column(nullable = false, length = 150)
    private String warehouseLocation;

    @Column(nullable = false)
    private Instant lastUpdated;

    @PrePersist
    void prePersist() {
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
        if (minimumStockLevel == null) {
            minimumStockLevel = 5;
        }
        lastUpdated = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        lastUpdated = Instant.now();
    }
}