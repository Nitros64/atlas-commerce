package com.atlascommerce.payment_service.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

        consumer = new InventoryEventConsumer(
                objectMapper,
                paymentEventPublisher
        );
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

        consumer.consume(payload);

        verify(paymentEventPublisher)
                .publishCompleted(any(PaymentCompletedEvent.class));
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

        consumer.consume(payload);

        verifyNoInteractions(paymentEventPublisher);
    }

    @Test
    void consume_shouldNotThrow_whenPayloadInvalid() {

        String payload = "{ INVALID_JSON";

        assertDoesNotThrow(() -> consumer.consume(payload));

        verifyNoInteractions(paymentEventPublisher);
    }

}