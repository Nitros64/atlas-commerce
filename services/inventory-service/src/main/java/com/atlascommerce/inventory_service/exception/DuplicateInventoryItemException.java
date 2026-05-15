package com.atlascommerce.inventory_service.exception;

public class DuplicateInventoryItemException extends RuntimeException {
    public DuplicateInventoryItemException(String message) {
        super(message);
    }
}