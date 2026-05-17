package com.atlascommerce.cart_service.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}