package com.atlascommerce.inventory_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.atlascommerce.inventory_service.event.InventoryReservedEvent;
import com.atlascommerce.inventory_service.event.InventoryReservedItemEvent;
import com.atlascommerce.inventory_service.event.OrderCreatedEvent;


@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryEventPublisher inventoryEventPublisher;

    @KafkaListener(
            topics = "${atlas.kafka.topics.order-events}",
            groupId = "inventory-service-v20"
    )
    public void consume(String payload) {

        try {

            OrderCreatedEvent event =
                    objectMapper.readValue(payload, OrderCreatedEvent.class);

            log.info(
                    "ORDER_CREATED received orderId={}",
                    event.orderId()
            );

            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                    event.orderId(),
                    event.userId(),
                    "RESERVED",
                    java.time.Instant.now().toString(),
                    event.items().stream()
                            .map(item -> new InventoryReservedItemEvent(
                                    item.productId(),
                                    item.quantity()
                            ))
                            .toList()
            );

            inventoryEventPublisher.publishReserved(reservedEvent);

        } catch (Exception e) {

            log.error("Failed to parse order event: {}", payload, e);
        }
    }
}