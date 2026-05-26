package com.atlascommerce.order.messaging.kafka;

import com.atlascommerce.order.event.InventoryFailedEvent;
import com.atlascommerce.order.event.InventoryReservedEvent;
import com.atlascommerce.order.event.PaymentCompletedEvent;
import com.atlascommerce.order.event.ShippingCreatedEvent;
import com.atlascommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(
            topics = "${atlas.kafka.topics.inventory-events}",
            groupId = "order-service-saga-v1"
    )
    public void consumeInventoryEvent(String payload) {
        try {

            var root = objectMapper.readTree(payload);
            String status = root.has("status") 
                            ? root.get("status").asString() 
                            : "";

            if ("RESERVED".equalsIgnoreCase(status)) {
                InventoryReservedEvent event =
                        objectMapper.readValue(payload, InventoryReservedEvent.class);

                orderService.markAsReserved(event.orderId());

                log.info("ORDER_STATUS_UPDATED orderId={} status=RESERVED", event.orderId());
                return;
            }                

            if ("FAILED".equalsIgnoreCase(status)) {
                InventoryFailedEvent event =
                        objectMapper.readValue(payload, InventoryFailedEvent.class);

                orderService.markAsFailed(event.orderId());

                log.warn("ORDER_STATUS_UPDATED orderId={} status=FAILED reason={}",
                        event.orderId(),
                        event.reason());

                return;
            }

            log.warn(
                "Ignoring unknown inventory event status={} payload={}",
                status,
                payload
            );

        } catch (Exception e) {
            log.error("Failed to process inventory event: {}", payload, e);
        }
    }

    @KafkaListener(
            topics = "${atlas.kafka.topics.payment-events}",
            groupId = "order-service-saga-v1"
    )
    public void consumePaymentEvent(String payload) {
        try {
            PaymentCompletedEvent event =
                    objectMapper.readValue(payload, PaymentCompletedEvent.class);

            if (orderService.isFailed(event.orderId())) {
                log.warn("Ignoring PAYMENT_COMPLETED for failed orderId={}", event.orderId());
                return;
            }        

            orderService.markAsPaid(event.orderId());

            log.info("ORDER_STATUS_UPDATED orderId={} status=PAID", event.orderId());

        } catch (Exception e) {
            log.error("Failed to process payment event: {}", payload, e);
        }
    }

    @KafkaListener(
            topics = "${atlas.kafka.topics.shipping-events}",
            groupId = "order-service-saga-v1"
    )
    public void consumeShippingEvent(String payload) {
        try {
            ShippingCreatedEvent event =
                    objectMapper.readValue(payload, ShippingCreatedEvent.class);

            if (orderService.isFailed(event.orderId())) {
                log.warn("Ignoring SHIPPING_CREATED for failed orderId={}", event.orderId());
                return;
            } 
            
            orderService.markAsShipped(event.orderId());

            log.info("ORDER_STATUS_UPDATED orderId={} status=SHIPPED", event.orderId());

        } catch (Exception e) {
            log.error("Failed to process shipping event: {}", payload, e);
        }
    }
}