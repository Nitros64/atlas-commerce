package com.atlascommerce.order.messaging.kafka;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.atlascommerce.order.event.OrderCreatedEvent;
import com.atlascommerce.order.event.OrderEventPublisher;

@Component
@Qualifier("kafkaOrderEventPublisher")
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String topic;

    public KafkaOrderEventPublisher(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            @Value("${atlas.kafka.topics.order-events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.getOrderId()), event);
    }
}