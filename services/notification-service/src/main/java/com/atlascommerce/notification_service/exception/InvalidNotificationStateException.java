package com.atlascommerce.notification_service.exception;

public class InvalidNotificationStateException extends RuntimeException {

    public InvalidNotificationStateException(String message) {
        super(message);
    }
}