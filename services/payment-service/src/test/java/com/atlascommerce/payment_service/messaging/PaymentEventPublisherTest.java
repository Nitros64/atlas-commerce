package com.atlascommerce.payment_service.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.atlascommerce.payment_service.event.PaymentCompletedEvent;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                publisher,
                "paymentEventsTopic",
                "payment-events"
        );
    }

    @Test
    void publishCompleted_shouldPublishKafkaEvent() throws Exception {
        PaymentCompletedEvent event =
                new PaymentCompletedEvent(
                        1L,
                        10L,
                        "COMPLETED",
                        BigDecimal.valueOf(99.99),
                        "EUR",
                        "2026-05-27T10:00:00Z"
                );

        String payload = """
                {"orderId":1}
                """;

        when(objectMapper.writeValueAsString(event))
                .thenReturn(payload);

        publisher.publishCompleted(event, "00-trace-id-span-id-01");

        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    @Test
    void publishCompleted_shouldThrow_whenSerializationFails() throws Exception {
        PaymentCompletedEvent event =
                new PaymentCompletedEvent(
                        1L,
                        10L,
                        "COMPLETED",
                        BigDecimal.valueOf(99.99),
                        "EUR",
                        "2026-05-27T10:00:00Z"
                );

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new RuntimeException("serialization error"));

        assertThrows(
                RuntimeException.class,
                () -> publisher.publishCompleted(event, "00-trace-id-span-id-01")
        );

        verifyNoInteractions(kafkaTemplate);
    }
}