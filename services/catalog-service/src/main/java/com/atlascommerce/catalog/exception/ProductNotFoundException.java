package com.atlascommerce.catalog.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long productId) {
        super("Product not found: " + productId);
    }
}