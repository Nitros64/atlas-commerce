package com.atlascommerce.shipping_service.messaging;

import com.atlascommerce.shipping_service.event.PaymentCompletedEvent;
import com.atlascommerce.shipping_service.event.ShippingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
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
    public void consume(ConsumerRecord<String, String> record) {

        String payload = record.value();
        String traceparent = getHeader(record, "traceparent");

        try {
            PaymentCompletedEvent event =
                    objectMapper.readValue(payload, PaymentCompletedEvent.class);

            log.info(
                    "PAYMENT_COMPLETED received orderId={} traceparent={}",
                    event.orderId(),
                    traceparent
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

            shippingEventPublisher.publishShippingCreated(shippingEvent);

        } catch (Exception e) {
            log.error("Failed to process payment event: {}", payload, e);
        }
    }

    private String getHeader(ConsumerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);

        if (header == null) {
            return null;
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }
}