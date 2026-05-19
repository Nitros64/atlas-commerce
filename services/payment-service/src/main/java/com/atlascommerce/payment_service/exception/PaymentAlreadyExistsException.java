package com.atlascommerce.payment_service.exception;

public class PaymentAlreadyExistsException extends RuntimeException {

    public PaymentAlreadyExistsException(String idempotencyKey) {
        super("Payment already exists for idempotencyKey: " + idempotencyKey);
    }
}