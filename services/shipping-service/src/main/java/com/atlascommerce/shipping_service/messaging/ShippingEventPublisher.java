package com.atlascommerce.shipping_service.messaging;

import com.atlascommerce.shipping_service.event.ShippingCreatedEvent;
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
}