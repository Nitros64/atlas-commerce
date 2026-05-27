package com.atlascommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CreateOrderRequest {

    @NotNull
    private final Long userId;

    @NotBlank
    private final String currency;

    @NotEmpty
    @Size(min = 1)
    @Valid
    private final List<CreateOrderItemRequest> items;

    public Long getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public List<CreateOrderItemRequest> getItems() {
        return items;
    }
}