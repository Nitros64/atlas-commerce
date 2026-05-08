package com.atlascommerce.catalog.service;

import com.atlascommerce.catalog.entity.ProductEntity;
import com.atlascommerce.catalog.event.OrderCreatedEvent;
import com.atlascommerce.catalog.event.OrderItemEvent;
import com.atlascommerce.catalog.exception.InsufficientStockException;
import com.atlascommerce.catalog.exception.ProductNotFoundException;
import com.atlascommerce.catalog.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void decreaseStock(OrderCreatedEvent event) {
        for (OrderItemEvent item : event.items()) {
            ProductEntity product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));

            if (product.getStock() < item.quantity()) {
                throw new InsufficientStockException(item.productId());
            }

            product.setStock(product.getStock() - item.quantity());
        }
    }
}