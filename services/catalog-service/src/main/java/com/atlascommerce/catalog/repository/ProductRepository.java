package com.atlascommerce.catalog.repository;

import com.atlascommerce.catalog.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}
