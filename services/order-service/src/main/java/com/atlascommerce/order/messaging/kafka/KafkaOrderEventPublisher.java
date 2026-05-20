package com.atlascommerce.order.messaging.kafka;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.atlascommerce.order.event.OrderCreatedEvent;
import com.atlascommerce.order.event.OrderEventPublisher;

import lombok.extern.slf4j.Slf4j;

@Component
@Qualifier("kafkaOrderEventPublisher")
@Slf4j
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String orderEventsTopic;

    public KafkaOrderEventPublisher(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            @Value("${atlas.kafka.topics.order-events}") String orderEventsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderEventsTopic = orderEventsTopic;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(orderEventsTopic, String.valueOf(event.orderId()), event);

        log.info("Published ORDER_CREATED event. orderId={} topic={}",
                event.orderId(),
                orderEventsTopic);
    }
}