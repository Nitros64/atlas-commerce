package com.atlascommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateOrderRequest {

    @NotNull
    private Long userId;

    @NotEmpty
    @Size(min = 1)
    @Valid
    private List<CreateOrderItemRequest> items;

    public CreateOrderRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public List<CreateOrderItemRequest> getItems() {
        return items;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setItems(List<CreateOrderItemRequest> items) {
        this.items = items;
    }
}