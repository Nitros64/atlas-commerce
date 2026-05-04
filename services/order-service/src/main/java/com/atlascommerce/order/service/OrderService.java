package com.atlascommerce.order.service;

import com.atlascommerce.order.dto.CreateOrderItemRequest;
import com.atlascommerce.order.dto.CreateOrderRequest;
import com.atlascommerce.order.dto.OrderItemResponse;
import com.atlascommerce.order.dto.OrderResponse;
import com.atlascommerce.order.entity.OrderEntity;
import com.atlascommerce.order.entity.OrderItemEntity;
import com.atlascommerce.order.event.OrderCreatedEvent;
import com.atlascommerce.order.messaging.OrderEventPublisher;
import com.atlascommerce.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        
    }

    public OrderResponse create(CreateOrderRequest request) {
        OrderEntity order = new OrderEntity();
        order.setUserId(request.getUserId());
        order.setStatus("PENDING");
        order.setCurrency("EUR");
        order.setCreatedAt(OffsetDateTime.now());

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            OrderItemEntity item = new OrderItemEntity();
            item.setProductId(itemRequest.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setOrder(order);

            order.getItems().add(item);

            BigDecimal lineTotal = itemRequest.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            total = total.add(lineTotal);
        }

        order.setTotalAmount(total);

        OrderEntity saved = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
        saved.getId(),
        saved.getUserId(),
        saved.getTotalAmount(),
        saved.getCurrency(),
        saved.getCreatedAt().toString(),
        saved.getItems().stream()
                .map(item -> new OrderCreatedEvent.OrderCreatedItem(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList()
        );

        try {
            orderEventPublisher.publishOrderCreated(event);
        } catch (Exception ex) {
            // temporalmente solo para desarrollo
            ex.printStackTrace();
        }

        return toResponse(saved);
    }

    public List<OrderResponse> findAll() {
        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse findById(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        return toResponse(order);
    }

    private OrderResponse toResponse(OrderEntity order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                items
        );
    }
}