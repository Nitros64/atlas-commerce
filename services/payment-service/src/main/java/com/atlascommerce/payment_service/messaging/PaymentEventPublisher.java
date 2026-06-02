package com.atlascommerce.payment_service.messaging;

import com.atlascommerce.payment_service.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${atlas.kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public void publishCompleted(PaymentCompletedEvent event, String traceparent) {
        publish(String.valueOf(event.orderId()), event, "PAYMENT_COMPLETED", traceparent);
    }

    private void publish(
            String key,
            PaymentCompletedEvent event,
            String eventName,
            String traceparent
    ) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(paymentEventsTopic, key, payload);

            if (traceparent != null && !traceparent.isBlank()) {
                record.headers().add(
                        "traceparent",
                        traceparent.getBytes(StandardCharsets.UTF_8)
                );
            }

            kafkaTemplate.send(record);

            log.info("{} published orderId={} topic={}",
                    eventName,
                    event.orderId(),
                    paymentEventsTopic);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish " + eventName, e);
        }
    }
}