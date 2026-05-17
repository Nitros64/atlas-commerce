package com.atlascommerce.coupon_service.mapper;

import com.atlascommerce.coupon_service.dto.CouponResponse;
import com.atlascommerce.coupon_service.entity.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getMaxUses(),
                coupon.getCurrentUses(),
                coupon.getActive(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }
}