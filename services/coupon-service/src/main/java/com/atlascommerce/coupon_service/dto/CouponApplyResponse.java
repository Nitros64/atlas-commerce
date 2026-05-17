package com.atlascommerce.coupon_service.dto;

import java.math.BigDecimal;

public record CouponApplyResponse(
        String code,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {
}