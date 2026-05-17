package com.atlascommerce.pricing_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record PricingResponse(
        List<PricingItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal total,
        String currency
) {
}