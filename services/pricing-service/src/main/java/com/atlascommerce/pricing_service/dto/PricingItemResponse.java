package com.atlascommerce.pricing_service.dto;

import java.math.BigDecimal;

public record PricingItemResponse(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}