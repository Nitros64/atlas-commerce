package com.atlascommerce.inventory_service.messaging;

import com.atlascommerce.inventory_service.event.InventoryFailedEvent;
import com.atlascommerce.inventory_service.event.InventoryReservedEvent;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${atlas.kafka.topics.inventory-events}")
    private String inventoryEventsTopic;

    public void publishReserved(InventoryReservedEvent event, String traceparent) {
        publish(String.valueOf(event.orderId()), event, "INVENTORY_RESERVED", traceparent);
    }

    public void publishFailed(InventoryFailedEvent event, String traceparent) {
        publish(String.valueOf(event.orderId()), event, "INVENTORY_FAILED", traceparent);
    }

    private void publish(String key, Object event, String eventName, String traceparent) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(inventoryEventsTopic, key, payload);

            if (traceparent != null && !traceparent.isBlank()) {
                record.headers().add(
                        "traceparent",
                        traceparent.getBytes(StandardCharsets.UTF_8)
                );
            }

            kafkaTemplate.send(record);

            log.info("{} published orderId={} topic={}",
                    eventName,
                    key,
                    inventoryEventsTopic);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish " + eventName, e);
        }
    }
}