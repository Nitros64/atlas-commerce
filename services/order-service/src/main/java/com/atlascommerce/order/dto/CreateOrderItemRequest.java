package com.atlascommerce.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderItemRequest {
    @NotNull
    private final Long productId;

    @NotNull
    @Min(1)
    private final Integer quantity;

    @NotNull
    @DecimalMin("0.01")
    private final BigDecimal unitPrice;

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    // public void setProductId(Long productId) {
    //     this.productId = productId;
    // }

    // public void setQuantity(Integer quantity) {
    //     this.quantity = quantity;
    // }

    // public void setUnitPrice(BigDecimal unitPrice) {
    //     this.unitPrice = unitPrice;
    // }
}
