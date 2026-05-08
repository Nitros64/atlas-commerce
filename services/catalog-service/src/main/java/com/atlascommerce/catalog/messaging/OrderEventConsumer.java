package com.atlascommerce.catalog.messaging;

import com.atlascommerce.catalog.event.OrderCreatedEvent;

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

    public OrderEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${atlas.kafka.topics.order-events:order-events}",
            groupId = "${spring.kafka.consumer.group-id:catalog-service}"
    )
    public void consume(String rawEvent) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(rawEvent, OrderCreatedEvent.class);

            log.info("OrderCreatedEvent received: orderId={}, userId={}, items={}",
                    event.orderId(),
                    event.userId(),
                    event.items());

        } catch (Exception ex) {
            log.error("Could not parse order event: {}", rawEvent, ex);
        }
    }

    @PostConstruct
    public void init() {
        log.info("OrderEventConsumer bean created");
    }
}
