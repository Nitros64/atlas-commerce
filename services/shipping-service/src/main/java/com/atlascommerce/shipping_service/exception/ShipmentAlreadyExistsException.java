package com.atlascommerce.shipping_service.exception;

public class ShipmentAlreadyExistsException extends RuntimeException {

    public ShipmentAlreadyExistsException(Long orderId) {
        super("Shipment already exists for orderId: " + orderId);
    }
}