package com.atlascommerce.cart_service.exception;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(Long userId) {
        super("Cart not found for userId: " + userId);
    }
}