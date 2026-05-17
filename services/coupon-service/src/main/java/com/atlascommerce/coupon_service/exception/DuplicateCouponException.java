package com.atlascommerce.coupon_service.exception;

public class DuplicateCouponException extends RuntimeException {

    public DuplicateCouponException(String code) {
        super("Coupon already exists with code: " + code);
    }
}