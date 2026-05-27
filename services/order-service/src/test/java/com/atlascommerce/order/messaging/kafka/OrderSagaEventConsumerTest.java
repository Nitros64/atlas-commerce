package com.atlascommerce.order.messaging.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.atlascommerce.order.service.OrderService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderSagaEventConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private DeadLetterPublisher deadLetterPublisher;

    private ObjectMapper objectMapper;

    private OrderSagaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        consumer = new OrderSagaEventConsumer(
                objectMapper,
                orderService,
                deadLetterPublisher
        );

        ReflectionTestUtils.setField(
                consumer,
                "inventoryEventsDltTopic",
                "inventory-events.DLT"
        );

        ReflectionTestUtils.setField(
                consumer,
                "paymentEventsDltTopic",
                "payment-events.DLT"
        );

        ReflectionTestUtils.setField(
                consumer,
                "shippingEventsDltTopic",
                "shipping-events.DLT"
        );
    }

    @Test
    void consumeInventoryEvent_shouldMarkAsReserved_whenStatusReserved() {
        String payload = """
                {
                  "orderId": 1,
                  "userId": 10,
                  "status": "RESERVED",
                  "createdAt": "2026-05-25T10:00:00Z",
                  "items": [
                    {
                      "productId": 101,
                      "quantity": 2
                    }
                  ]
                }
                """;

        consumer.consumeInventoryEvent(payload);

        verify(orderService).markAsReserved(1L);
        verify(orderService, never()).markAsFailed(anyLong());
        verifyNoInteractions(deadLetterPublisher);
    }

    @Test
    void consumeInventoryEvent_shouldMarkAsFailed_whenStatusFailed() {
        String payload = """
                {
                  "orderId": 1,
                  "userId": 10,
                  "status": "FAILED",
                  "reason": "Inventory validation failed",
                  "createdAt": "2026-05-25T10:00:00Z",
                  "items": [
                    {
                      "productId": 999999,
                      "requestedQuantity": 1,
                      "reason": "Invalid product or quantity"
                    }
                  ]
                }
                """;

        consumer.consumeInventoryEvent(payload);

        verify(orderService).markAsFailed(1L);
        verify(orderService, never()).markAsReserved(anyLong());
        verifyNoInteractions(deadLetterPublisher);
    }

    @Test
    void consumeInventoryEvent_shouldPublishToDlt_whenPayloadIsInvalid() {
        String payload = "{ INVALID_JSON";

        consumer.consumeInventoryEvent(payload);

        verify(deadLetterPublisher).publish(
                eq("inventory-events.DLT"),
                isNull(),
                eq(payload),
                any(Exception.class)
        );

        verifyNoInteractions(orderService);
    }

    @Test
    void consumePaymentEvent_shouldMarkAsPaid() {
        String payload = """
                {
                  "orderId": 1,
                  "paymentId": "PAY-123",
                  "status": "COMPLETED"
                }
                """;

        when(orderService.isFailed(1L)).thenReturn(false);

        consumer.consumePaymentEvent(payload);

        verify(orderService).markAsPaid(1L);
        verifyNoInteractions(deadLetterPublisher);
    }

    @Test
    void consumePaymentEvent_shouldIgnore_whenOrderFailed() {
        String payload = """
                {
                  "orderId": 1,
                  "paymentId": "PAY-123",
                  "status": "COMPLETED"
                }
                """;

        when(orderService.isFailed(1L)).thenReturn(true);

        consumer.consumePaymentEvent(payload);

        verify(orderService, never()).markAsPaid(anyLong());
        verifyNoInteractions(deadLetterPublisher);
    }

    @Test
    void consumePaymentEvent_shouldPublishToDlt_whenPayloadInvalid() {
        String payload = "{ INVALID_JSON";

        consumer.consumePaymentEvent(payload);

        verify(deadLetterPublisher).publish(
                eq("payment-events.DLT"),
                isNull(),
                eq(payload),
                any(Exception.class)
        );
    }

    @Test
    void consumeShippingEvent_shouldMarkAsShipped() {
        String payload = """
                {
                  "orderId": 1,
                  "shipmentId": "SHIP-123",
                  "status": "CREATED"
                }
                """;

        when(orderService.isFailed(1L)).thenReturn(false);

        consumer.consumeShippingEvent(payload);

        verify(orderService).markAsShipped(1L);
        verifyNoInteractions(deadLetterPublisher);
    }
    
    @Test
    void consumeShippingEvent_shouldIgnore_whenOrderFailed() {
        String payload = """
                {
                  "orderId": 1,
                  "shipmentId": "SHIP-123",
                  "status": "CREATED"
                }
                """;

        when(orderService.isFailed(1L)).thenReturn(true);

        consumer.consumeShippingEvent(payload);

        verify(orderService, never()).markAsShipped(anyLong());
        verifyNoInteractions(deadLetterPublisher);
    }

    @Test
    void consumeShippingEvent_shouldPublishToDlt_whenPayloadInvalid() {
        String payload = "{ INVALID_JSON";

        consumer.consumeShippingEvent(payload);

        verify(deadLetterPublisher).publish(
                eq("shipping-events.DLT"),
                isNull(),
                eq(payload),
                any(Exception.class)
        );
    }

}