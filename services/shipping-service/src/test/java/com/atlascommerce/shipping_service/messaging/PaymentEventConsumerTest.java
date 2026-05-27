package com.atlascommerce.shipping_service.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atlascommerce.shipping_service.event.ShippingCreatedEvent;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private ShippingEventPublisher shippingEventPublisher;

    private ObjectMapper objectMapper;

    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        consumer = new PaymentEventConsumer(
                objectMapper,
                shippingEventPublisher
        );
    }

    @Test
    void consume_shouldPublishShippingCreated_whenPaymentCompleted() {
        String payload = """
                {
                  "orderId": 1,
                  "userId": 10,
                  "status": "COMPLETED",
                  "amount": 99.99,
                  "currency": "EUR",
                  "createdAt": "2026-05-27T10:00:00Z"
                }
                """;

        consumer.consume(payload);

        verify(shippingEventPublisher)
                .publishShippingCreated(any(ShippingCreatedEvent.class));
    }

    @Test
    void consume_shouldNotThrow_whenPayloadInvalid() {
        String payload = "{ INVALID_JSON";

        assertDoesNotThrow(() -> consumer.consume(payload));

        verifyNoInteractions(shippingEventPublisher);
    }
}