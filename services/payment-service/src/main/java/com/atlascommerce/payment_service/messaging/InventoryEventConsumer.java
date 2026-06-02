package com.atlascommerce.payment_service.messaging;

import com.atlascommerce.payment_service.event.InventoryReservedEvent;
import com.atlascommerce.payment_service.event.PaymentCompletedEvent;
import com.atlascommerce.payment_service.observability.KafkaTracingHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentEventPublisher paymentEventPublisher;
    private final KafkaTracingHelper kafkaTracingHelper;

    @KafkaListener(
            topics = "${atlas.kafka.topics.inventory-events}",
            groupId = "payment-service-v1"
    )
    public void consume(ConsumerRecord<String, String> record) {
        
        String payload = record.value();
        
        try(var ignored = kafkaTracingHelper.startConsumerSpan(record, "kafka consume order-events")) {
            
            String traceparent = getHeader(record, "traceparent");

            var root = objectMapper.readTree(payload);

            String status = root.has("status")
                    ? root.get("status").asString()
                    : "";

            if (!"RESERVED".equalsIgnoreCase(status)) {
                log.warn("Ignoring inventory event with status={} payload={}", status, payload);
                return;
            }
            
            InventoryReservedEvent event =
                    objectMapper.readValue(payload, InventoryReservedEvent.class);

            log.info(
                    "INVENTORY_RESERVED received orderId={} traceparent={}",
                    event.orderId(),
                    traceparent
                );

            PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                    event.orderId(),
                    event.userId(),
                    "COMPLETED",
                    BigDecimal.ZERO,
                    "EUR",
                    Instant.now().toString()
            );

            paymentEventPublisher.publishCompleted(completedEvent, traceparent);

        } catch (Exception e) {
            log.error("Failed to process inventory event: {}", payload, e);
        }
    }

    private String getHeader(
            ConsumerRecord<String, String> record,
            String name) {

        var header = record.headers().lastHeader(name);

        if (header == null) {
            return null;
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }
}