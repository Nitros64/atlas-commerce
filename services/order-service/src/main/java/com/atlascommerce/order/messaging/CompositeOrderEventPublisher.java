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

    // public void publishOrderCreated(OrderCreatedEvent event) {
    //     log.info("Publishing order.created event for orderId={}", event.getOrderId());
        
    //     rabbitTemplate.convertAndSend(
    //             RabbitConfig.ORDER_EXCHANGE,
    //             RabbitConfig.ORDER_CREATED_ROUTING_KEY,
    //             event
    //     );

    //     log.info("Published order.created event for orderId={}", event.getOrderId());
    // }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitPublisher.publishOrderCreated(event);
        log.info("Published order.created event for RabbitMq");
        kafkaPublisher.publishOrderCreated(event);
        log.info("Published order.created event for Kafka");
    }
}