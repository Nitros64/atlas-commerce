package com.atlascommerce.notification_service.messaging;

import com.atlascommerce.notification_service.event.ShippingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${atlas.kafka.topics.shipping-events}",
            groupId = "notification-service-v1"
    )
    public void consume(String payload) {
        try {
            ShippingCreatedEvent event =
                    objectMapper.readValue(payload, ShippingCreatedEvent.class);

            log.info(
                    "NOTIFICATION_SENT orderId={} userId={} carrier={} trackingNumber={}",
                    event.orderId(),
                    event.userId(),
                    event.carrier(),
                    event.trackingNumber()
            );

        } catch (Exception e) {
            log.error("Failed to process shipping event: {}", payload, e);
        }
    }
}