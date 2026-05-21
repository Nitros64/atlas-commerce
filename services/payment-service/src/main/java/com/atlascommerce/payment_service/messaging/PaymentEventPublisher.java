package com.atlascommerce.payment_service.messaging;

import com.atlascommerce.payment_service.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${atlas.kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public void publishCompleted(PaymentCompletedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    paymentEventsTopic,
                    String.valueOf(event.orderId()),
                    payload
            );

            log.info("PAYMENT_COMPLETED published orderId={} topic={}",
                    event.orderId(),
                    paymentEventsTopic);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish PAYMENT_COMPLETED", e);
        }
    }
}