package com.atlascommerce.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atlascommerce.order.dto.CreateOrderItemRequest;
import com.atlascommerce.order.dto.CreateOrderRequest;
import com.atlascommerce.order.dto.OrderResponse;
import com.atlascommerce.order.entity.OrderEntity;
import com.atlascommerce.order.event.OrderCreatedEvent;
import com.atlascommerce.order.event.OrderEventPublisher;
import com.atlascommerce.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void markAsReserved_shouldUpdateStatus_whenOrderIsPending() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markAsReserved(1L);

        assertEquals("RESERVED", order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void markAsReserved_shouldNotUpdate_whenOrderIsFailed() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setStatus("FAILED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markAsReserved(1L);

        assertEquals("FAILED", order.getStatus());
        verify(orderRepository, never()).save(order);
    }

    @Test
    void markAsPaid_shouldUpdateStatus_whenOrderIsReserved() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setStatus("RESERVED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markAsPaid(1L);

        assertEquals("PAID", order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void markAsPaid_shouldNotUpdate_whenOrderIsPending() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markAsPaid(1L);

        assertEquals("PENDING", order.getStatus());
        verify(orderRepository, never()).save(order);
    }

    @Test
    void markAsShipped_shouldUpdateStatus_whenOrderIsPaid() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setStatus("PAID");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markAsShipped(1L);

        assertEquals("SHIPPED", order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void markAsFailed_shouldAlwaysUpdateToFailed() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.markAsFailed(1L);

        assertEquals("FAILED", order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void isFailed_shouldReturnTrue_whenOrderStatusIsFailed() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setStatus("FAILED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertTrue(orderService.isFailed(1L));
    }

    @Test
  void create_shouldCreatePendingOrderAndPublishEvent() {
      CreateOrderRequest request = new CreateOrderRequest(
              1L,
              "EUR",
              List.of(
                      new CreateOrderItemRequest(101L, 2, BigDecimal.valueOf(19.99)),
                      new CreateOrderItemRequest(202L, 1, BigDecimal.valueOf(49.90))
              )
      );

      when(orderRepository.save(any(OrderEntity.class)))
              .thenAnswer(invocation -> {
                  OrderEntity order = invocation.getArgument(0);
                  order.setId(10L);
                  return order;
              });

      OrderResponse response = orderService.create(request);

      assertEquals(10L, response.id());
      assertEquals(1L, response.userId());
      assertEquals("PENDING", response.status());
      assertEquals("EUR", response.currency());
      assertEquals(0, BigDecimal.valueOf(89.88).compareTo(response.totalAmount()));
      assertEquals(2, response.items().size());

      verify(orderRepository).save(any(OrderEntity.class));
      verify(orderEventPublisher).publishOrderCreated(any(OrderCreatedEvent.class));
  }
}