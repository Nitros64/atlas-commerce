package com.atlascommerce.payment_service.messaging;

import com.atlascommerce.payment_service.event.InventoryReservedEvent;
import com.atlascommerce.payment_service.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentEventPublisher paymentEventPublisher;

    @KafkaListener(
            topics = "${atlas.kafka.topics.inventory-events}",
            groupId = "payment-service-v1"
    )
    public void consume(String payload) {
        try {
            InventoryReservedEvent event =
                    objectMapper.readValue(payload, InventoryReservedEvent.class);

            log.info("INVENTORY_RESERVED received orderId={}", event.orderId());

            PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                    event.orderId(),
                    event.userId(),
                    "COMPLETED",
                    BigDecimal.ZERO,
                    "EUR",
                    Instant.now().toString()
            );

            paymentEventPublisher.publishCompleted(completedEvent);

        } catch (Exception e) {
            log.error("Failed to process inventory event: {}", payload, e);
        }
    }
}