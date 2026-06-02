package com.atlascommerce.inventory_service.messaging;

import com.atlascommerce.inventory_service.event.InventoryFailedEvent;
import com.atlascommerce.inventory_service.event.InventoryFailedItemEvent;
import com.atlascommerce.inventory_service.event.InventoryReservedEvent;
import com.atlascommerce.inventory_service.event.InventoryReservedItemEvent;
import com.atlascommerce.inventory_service.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryEventPublisher inventoryEventPublisher;

    @KafkaListener(topics = "${atlas.kafka.topics.order-events}", groupId = "inventory-service-v20")
    public void consume(ConsumerRecord<String, String> record) {

        String payload = record.value();
        String traceparent = getHeader(record, "traceparent");

        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);

            log.info("ORDER_CREATED received orderId={} traceparent={}", event.orderId(), traceparent);

            boolean hasInvalidItem = event.items().stream()
                    .anyMatch(item -> item.productId() == null
                            || item.productId() == 999999L
                            || item.quantity() == null
                            || item.quantity() <= 0);

            if (hasInvalidItem) {
                InventoryFailedEvent failedEvent = new InventoryFailedEvent(
                        event.orderId(),
                        event.userId(),
                        "FAILED",
                        "Inventory validation failed",
                        java.time.Instant.now().toString(),
                        event.items().stream()
                                .filter(item -> item.productId() == null
                                        || item.productId() == 999999L
                                        || item.quantity() == null
                                        || item.quantity() <= 0)
                                .map(item -> new InventoryFailedItemEvent(
                                        item.productId(),
                                        item.quantity(),
                                        "Invalid product or quantity"))
                                .toList());

                inventoryEventPublisher.publishFailed(failedEvent, traceparent);

                log.warn("INVENTORY_FAILED orderId={}", event.orderId());
                return;
            }

            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                    event.orderId(),
                    event.userId(),
                    "RESERVED",
                    java.time.Instant.now().toString(),
                    event.items().stream()
                            .map(item -> new InventoryReservedItemEvent(
                                    item.productId(),
                                    item.quantity()))
                            .toList());

            inventoryEventPublisher.publishReserved(reservedEvent, traceparent);

        } catch (Exception e) {
            log.error("Failed to parse order event: {}", payload, e);
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