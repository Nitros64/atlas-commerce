package com.atlascommerce.audit_service.enums;

public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    USER_CREATED,
    ORDER_CREATED,
    PAYMENT_CAPTURED,
    PAYMENT_FAILED,
    COUPON_APPLIED,
    SHIPMENT_CREATED,
    SHIPMENT_DELIVERED
}