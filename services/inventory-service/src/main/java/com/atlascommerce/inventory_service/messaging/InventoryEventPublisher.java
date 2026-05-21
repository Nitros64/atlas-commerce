package com.atlascommerce.inventory_service.messaging;

import com.atlascommerce.inventory_service.event.InventoryFailedEvent;
import com.atlascommerce.inventory_service.event.InventoryReservedEvent;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${atlas.kafka.topics.inventory-events}")
    private String inventoryEventsTopic;

    public void publishReserved(InventoryReservedEvent event) {
        publish(String.valueOf(event.orderId()), event, "INVENTORY_RESERVED");
    }

    public void publishFailed(InventoryFailedEvent event) {
        publish(String.valueOf(event.orderId()), event, "INVENTORY_FAILED");
    }

    private void publish(String key, Object event, String eventName) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(inventoryEventsTopic, key, payload);

            log.info("{} published orderId={} topic={}",
                    eventName,
                    key,
                    inventoryEventsTopic);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish " + eventName, e);
        }
    }
}