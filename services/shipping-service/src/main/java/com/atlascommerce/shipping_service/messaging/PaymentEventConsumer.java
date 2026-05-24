package com.atlascommerce.shipping_service.messaging;

import com.atlascommerce.shipping_service.event.PaymentCompletedEvent;
import com.atlascommerce.shipping_service.event.ShippingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final ShippingEventPublisher shippingEventPublisher;

    @KafkaListener(
            topics = "${atlas.kafka.topics.payment-events}",
            groupId = "shipping-service-v1"
    )
    public void consume(String payload) {

        try {

            PaymentCompletedEvent event =
                    objectMapper.readValue(
                            payload,
                            PaymentCompletedEvent.class
                    );

            log.info(
                    "PAYMENT_COMPLETED received orderId={}",
                    event.orderId()
            );

            ShippingCreatedEvent shippingEvent =
                    new ShippingCreatedEvent(
                            event.orderId(),
                            event.userId(),
                            "CREATED",
                            UUID.randomUUID().toString(),
                            "DHL",
                            Instant.now().toString()
                    );

            shippingEventPublisher
                    .publishShippingCreated(shippingEvent);

        } catch (Exception e) {

            log.error(
                    "Failed to process payment event: {}",
                    payload,
                    e
            );
        }
    }
}