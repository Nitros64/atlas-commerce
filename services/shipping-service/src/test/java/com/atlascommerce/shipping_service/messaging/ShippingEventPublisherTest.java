package com.atlascommerce.shipping_service.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.atlascommerce.shipping_service.event.ShippingCreatedEvent;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShippingEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ShippingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                publisher,
                "shippingEventsTopic",
                "shipping-events"
        );
    }

    @Test
    void publishShippingCreated_shouldPublishKafkaEvent() throws Exception {
        ShippingCreatedEvent event =
                new ShippingCreatedEvent(
                        1L,
                        10L,
                        "CREATED",
                        "TRACK-123",
                        "DHL",
                        "2026-05-27T10:00:00Z"
                );

        String payload = """
                {"orderId":1}
                """;

        when(objectMapper.writeValueAsString(event))
                .thenReturn(payload);

        publisher.publishShippingCreated(event);

        verify(kafkaTemplate).send(
                "shipping-events",
                "1",
                payload
        );
    }

    @Test
    void publishShippingCreated_shouldThrow_whenSerializationFails() throws Exception {
        ShippingCreatedEvent event =
                new ShippingCreatedEvent(
                        1L,
                        10L,
                        "CREATED",
                        "TRACK-123",
                        "DHL",
                        "2026-05-27T10:00:00Z"
                );

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new RuntimeException("serialization error"));

        assertThrows(
                RuntimeException.class,
                () -> publisher.publishShippingCreated(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }
}