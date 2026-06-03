package com.atlascommerce.order.messaging.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.atlascommerce.order.config.RabbitConfig;
import com.atlascommerce.order.event.OrderCreatedEvent;
import com.atlascommerce.order.event.OrderEventPublisher;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Component
@Qualifier("rabbitOrderEventPublisher")
@Slf4j
public class RabbitOrderEventPublisher implements OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public RabbitOrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @CircuitBreaker(name = "rabbitOrderPublisher", fallbackMethod = "fallbackPublishOrderCreated")
    @Retry(name = "rabbitOrderPublisher")
    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_CREATED_ROUTING_KEY,
                event
        );
        log.info("Published order.created event for RabbitMq orderId={}", event.orderId());
    }

    private void fallbackPublishOrderCreated(OrderCreatedEvent event, Throwable ex) {
        log.warn(
            "RabbitMQ circuit breaker fallback. orderId={} error={}",
            event.orderId(),
            ex.getMessage()
        );
    }
}
