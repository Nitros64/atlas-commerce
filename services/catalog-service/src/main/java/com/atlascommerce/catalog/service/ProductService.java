package com.atlascommerce.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.atlascommerce.catalog.dto.CreateProductRequest;
import com.atlascommerce.catalog.dto.ProductResponse;
import com.atlascommerce.catalog.entity.ProductEntity;
import com.atlascommerce.catalog.repository.ProductRepository;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(CreateProductRequest request) {
        ProductEntity entity = new ProductEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setPrice(request.getPrice());
        entity.setStock(request.getStock());

        ProductEntity saved = productRepository.save(entity);
        return toResponse(saved);
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findById(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        return toResponse(product);
    }

    private ProductResponse toResponse(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getStock()
        );
    }
}
