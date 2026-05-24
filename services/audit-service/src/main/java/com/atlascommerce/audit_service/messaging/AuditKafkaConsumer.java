package com.atlascommerce.audit_service.messaging;

import com.atlascommerce.audit_service.dto.CreateAuditEventRequest;
import com.atlascommerce.audit_service.enums.AuditEventType;
import com.atlascommerce.audit_service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditKafkaConsumer {

    private final AuditEventService auditEventService;

    @KafkaListener(
        topics = {
                "${atlas.kafka.topics.order-events}",
                "${atlas.kafka.topics.inventory-events}",
                "${atlas.kafka.topics.payment-events}",
                "${atlas.kafka.topics.shipping-events}"
        },
        groupId = "audit-service-v1"
    )
    public void consume(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {

        log.info(
                "AUDIT_EVENT received topic={} key={}",
                topic,
                key
        );

        AuditEventType eventType = resolveEventType(topic);

        CreateAuditEventRequest request =
                new CreateAuditEventRequest(
                        null,
                        eventType,
                        "KAFKA_EVENT",
                        topic,
                        payload
                );

        auditEventService.create(request);
    }

    private AuditEventType resolveEventType(String topic) {

        return switch (topic) {

            case "order-events" ->
                    AuditEventType.ORDER_CREATED;

            case "inventory-events" ->
                    AuditEventType.INVENTORY_RESERVED;

            case "payment-events" ->
                    AuditEventType.PAYMENT_CAPTURED;

            case "shipping-events" ->
                    AuditEventType.SHIPMENT_CREATED;

            default ->
                    throw new IllegalArgumentException(
                            "Unknown topic: " + topic
                    );
        };
    }
}