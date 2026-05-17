package com.atlascommerce.pricing_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record PricingRequest(
        @NotEmpty List<@Valid PricingItemRequest> items,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal discountPercentage,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal taxPercentage,

        @NotBlank String currency
) {
}