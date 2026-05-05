package com.atlascommerce.order.repository;

import com.atlascommerce.order.entity.OrderEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findAll();

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findById(Long id);
}