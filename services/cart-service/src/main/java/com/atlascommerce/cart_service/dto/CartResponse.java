package com.atlascommerce.cart_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        Long id,
        Long userId,
        List<CartItemResponse> items,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
) {
}