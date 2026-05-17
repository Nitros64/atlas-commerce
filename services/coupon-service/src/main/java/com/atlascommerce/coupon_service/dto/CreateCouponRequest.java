package com.atlascommerce.coupon_service.dto;

import com.atlascommerce.coupon_service.enums.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponRequest(
        @NotBlank String code,

        @NotNull DiscountType discountType,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal discountValue,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal minimumOrderAmount,

        @NotNull
        @Min(1)
        Integer maxUses,

        @NotNull Instant validFrom,

        @NotNull Instant validUntil
) {
}