package com.atlascommerce.order.messaging.kafka;

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
            InventoryReservedEvent event =
                    objectMapper.readValue(payload, InventoryReservedEvent.class);

            orderService.markAsReserved(event.orderId());

            log.info("ORDER_STATUS_UPDATED orderId={} status=RESERVED", event.orderId());

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

            orderService.markAsShipped(event.orderId());

            log.info("ORDER_STATUS_UPDATED orderId={} status=SHIPPED", event.orderId());

        } catch (Exception e) {
            log.error("Failed to process shipping event: {}", payload, e);
        }
    }
}