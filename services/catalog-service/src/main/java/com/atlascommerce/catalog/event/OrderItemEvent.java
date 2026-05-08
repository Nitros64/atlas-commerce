package com.atlascommerce.catalog.event;

import java.math.BigDecimal;

public record OrderItemEvent(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {
}