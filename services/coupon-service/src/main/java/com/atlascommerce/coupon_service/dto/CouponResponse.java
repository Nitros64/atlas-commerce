package com.atlascommerce.coupon_service.dto;

import com.atlascommerce.coupon_service.enums.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        Integer maxUses,
        Integer currentUses,
        Boolean active,
        Instant validFrom,
        Instant validUntil,
        Instant createdAt,
        Instant updatedAt
) {
}