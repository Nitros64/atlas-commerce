package com.atlascommerce.payment_service.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atlascommerce.payment_service.event.PaymentCompletedEvent;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private ObjectMapper objectMapper;
    private InventoryEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new InventoryEventConsumer(objectMapper, paymentEventPublisher);
    }

    @Test
    void consume_shouldPublishPaymentCompleted_whenInventoryReserved() {
        String payload = """
                {
                  "orderId": 1,
                  "userId": 10,
                  "status": "RESERVED",
                  "createdAt": "2026-05-27T10:00:00Z",
                  "items": [
                    {
                      "productId": 101,
                      "quantity": 2
                    }
                  ]
                }
                """;

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("inventory-events", 0, 0L, "1", payload);

        record.headers().add("traceparent", "00-trace-id-span-id-01".getBytes());

        consumer.consume(record);

        verify(paymentEventPublisher)
                .publishCompleted(any(PaymentCompletedEvent.class), eq("00-trace-id-span-id-01"));
    }

    @Test
    void consume_shouldIgnore_whenStatusIsFailed() {
        String payload = """
                {
                  "orderId": 1,
                  "userId": 10,
                  "status": "FAILED"
                }
                """;

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("inventory-events", 0, 0L, "1", payload);

        consumer.consume(record);

        verifyNoInteractions(paymentEventPublisher);
    }

    @Test
    void consume_shouldNotThrow_whenPayloadInvalid() {
        String payload = "{ INVALID_JSON";

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("inventory-events", 0, 0L, "1", payload);

        assertDoesNotThrow(() -> consumer.consume(record));

        verifyNoInteractions(paymentEventPublisher);
    }
}