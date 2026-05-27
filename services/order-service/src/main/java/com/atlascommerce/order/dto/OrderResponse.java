package com.atlascommerce.order.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String status,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime createdAt,
        List<OrderItemResponse> items
) {
}