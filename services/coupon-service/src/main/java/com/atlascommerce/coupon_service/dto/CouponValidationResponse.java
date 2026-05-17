package com.atlascommerce.coupon_service.dto;

public record CouponValidationResponse(
        String code,
        boolean valid,
        String reason
) {
}