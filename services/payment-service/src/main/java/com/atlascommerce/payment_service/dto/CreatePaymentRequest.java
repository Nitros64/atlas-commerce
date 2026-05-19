package com.atlascommerce.payment_service.dto;

import com.atlascommerce.payment_service.enums.PaymentProvider;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull Long orderId,
        @NotNull Long userId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotBlank String currency,

        @NotNull PaymentProvider provider,

        @NotBlank String idempotencyKey
) {
}