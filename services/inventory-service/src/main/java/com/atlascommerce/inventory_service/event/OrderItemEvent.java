package com.atlascommerce.inventory_service.event;

import java.math.BigDecimal;

public record OrderItemEvent(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {
}
