package com.atlascommerce.order.messaging;

import com.atlascommerce.order.event.OrderCreatedEvent;
import com.atlascommerce.order.event.OrderEventPublisher;
import com.atlascommerce.order.messaging.kafka.KafkaOrderEventPublisher;
import com.atlascommerce.order.messaging.rabbitmq.RabbitOrderEventPublisher;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Primary
public class CompositeOrderEventPublisher implements OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(CompositeOrderEventPublisher.class);
    
    private final RabbitOrderEventPublisher rabbitPublisher;
    private final KafkaOrderEventPublisher kafkaPublisher;

    public CompositeOrderEventPublisher(RabbitOrderEventPublisher rabbitPublisher, KafkaOrderEventPublisher kafkaPublisher) {
        this.rabbitPublisher = rabbitPublisher;
        this.kafkaPublisher = kafkaPublisher;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            rabbitPublisher.publishOrderCreated(event);
        } catch (Exception e) {
            log.warn(
                    "RabbitMQ publish failed, continuing with Kafka. orderId={} error={}",
                    event.orderId(),
                    e.getMessage()
            );
        }

        kafkaPublisher.publishOrderCreated(event);
        log.info("Published order.created event for Kafka");
    }
}