package main.java.com.atlascommerce.common.events.order;

import java.math.BigDecimal;

public record OrderItemEvent(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {
}