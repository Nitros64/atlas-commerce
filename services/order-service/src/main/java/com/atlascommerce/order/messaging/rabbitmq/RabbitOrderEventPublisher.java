package com.atlascommerce.order.messaging.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.atlascommerce.order.config.RabbitConfig;
import com.atlascommerce.order.event.OrderCreatedEvent;
import com.atlascommerce.order.event.OrderEventPublisher;

@Component
@Qualifier("rabbitOrderEventPublisher")
public class RabbitOrderEventPublisher implements OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public RabbitOrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_CREATED_ROUTING_KEY,
                event
        );
    }
}
