package com.atlascommerce.order.event;

public interface OrderEventPublisher {
    void publishOrderCreated(OrderCreatedEvent event);
}
