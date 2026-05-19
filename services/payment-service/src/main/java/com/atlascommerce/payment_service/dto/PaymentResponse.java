package com.atlascommerce.payment_service.dto;

import com.atlascommerce.payment_service.enums.PaymentProvider;
import com.atlascommerce.payment_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentProvider provider,
        String idempotencyKey,
        String providerTransactionId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}