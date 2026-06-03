package com.atlascommerce.shipping_service.messaging;

import com.atlascommerce.shipping_service.event.ShippingCreatedEvent;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${atlas.kafka.topics.shipping-events}")
    private String shippingEventsTopic;

    @CircuitBreaker(
            name = "shippingKafkaPublisher",
            fallbackMethod = "fallbackPublish"
    )
    @Retry(name = "shippingKafkaPublisher")
    public void publishShippingCreated(ShippingCreatedEvent event) {

        try {

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    shippingEventsTopic,
                    String.valueOf(event.orderId()),
                    payload
            );

            log.info(
                    "SHIPPING_CREATED published orderId={} topic={}",
                    event.orderId(),
                    shippingEventsTopic
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to publish SHIPPING_CREATED",
                    e
            );
        }
    }

    @SuppressWarnings("unused")
    private void fallbackPublish(String key, Object event, String eventName, String traceparent,
            Throwable ex) {

        log.error(
                "{} Kafka publish fallback. orderId={} topic={} error={}",
                eventName,
                key,
                shippingEventsTopic,
                ex.getMessage()
        );
    }
}