package com.atlascommerce.order.messaging.kafka;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.atlascommerce.order.event.OrderCreatedEvent;
import com.atlascommerce.order.event.OrderEventPublisher;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@Qualifier("kafkaOrderEventPublisher")
@Slf4j
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String orderEventsTopic;
    private final ObjectMapper objectMapper;


    public KafkaOrderEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${atlas.kafka.topics.order-events}") String orderEventsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderEventsTopic = orderEventsTopic;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        // kafkaTemplate.send(orderEventsTopic, String.valueOf(event.orderId()), event);

        // log.info("Published ORDER_CREATED event. orderId={} topic={}",
        //         event.orderId(),
        //         orderEventsTopic);
        try {

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    orderEventsTopic,
                    String.valueOf(event.orderId()),
                    payload
            );

            log.info("Published ORDER_CREATED orderId={}", event.orderId());

        } catch (Exception e) {

            throw new RuntimeException("Failed to publish event", e);
        }
    }
}