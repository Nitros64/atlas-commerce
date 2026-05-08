package com.atlascommerce.catalog.messaging;

import com.atlascommerce.catalog.event.OrderCreatedEvent;
import com.atlascommerce.catalog.exception.InsufficientStockException;
import com.atlascommerce.catalog.exception.ProductNotFoundException;
import com.atlascommerce.catalog.service.InventoryService;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;

    public OrderEventConsumer(ObjectMapper objectMapper, InventoryService inventoryService) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
    }

    @KafkaListener(
        topics = "${atlas.kafka.topics.order-events:order-events}",
        groupId = "${spring.kafka.consumer.group-id:catalog-service}"
    )
    public void consume(String rawEvent) {
        OrderCreatedEvent event;

        try {
            event = objectMapper.readValue(rawEvent, OrderCreatedEvent.class);
        } catch (Exception ex) {
            log.error("Could not parse order event: {}", rawEvent, ex);
            throw new IllegalArgumentException("Invalid order event JSON", ex);
        }

        try {
            inventoryService.decreaseStock(event);
            log.info("Stock decreased successfully for orderId={}", event.orderId());

        } catch (ProductNotFoundException | InsufficientStockException ex) {
            log.warn("Business error processing orderId={}. message={}",
                    event.orderId(),
                    ex.getMessage());
            throw ex;
        }
    }

    @PostConstruct
    public void init() {
        log.info("OrderEventConsumer bean created");
    }
}
