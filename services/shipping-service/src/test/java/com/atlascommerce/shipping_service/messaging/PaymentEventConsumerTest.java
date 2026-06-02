package com.atlascommerce.shipping_service.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atlascommerce.shipping_service.event.ShippingCreatedEvent;
import com.atlascommerce.shipping_service.observability.KafkaTracingHelper;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private ShippingEventPublisher shippingEventPublisher;

    @Mock
    private KafkaTracingHelper kafkaTracingHelper;

    private ObjectMapper objectMapper;

    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        consumer = new PaymentEventConsumer(objectMapper, shippingEventPublisher, kafkaTracingHelper);
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

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(
                        "payment-events",
                        0,
                        0L,
                        "1",
                        payload
                );

        record.headers().add(
                "traceparent",
                "00-trace-id-span-id-01".getBytes()
        );

        consumer.consume(record);

        verify(shippingEventPublisher)
                .publishShippingCreated(any(ShippingCreatedEvent.class));
    }

    @Test
    void consume_shouldNotThrow_whenPayloadInvalid() {

        String payload = "{ INVALID_JSON";

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(
                        "payment-events",
                        0,
                        0L,
                        "1",
                        payload
                );

        assertDoesNotThrow(() -> consumer.consume(record));

        verifyNoInteractions(shippingEventPublisher);
    }
}