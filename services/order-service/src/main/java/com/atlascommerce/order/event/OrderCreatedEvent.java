package com.atlascommerce.order.event;

import java.math.BigDecimal;
import java.util.List;

public class OrderCreatedEvent {

    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String currency;
    private String createdAt;
    private List<OrderCreatedItem> items;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(
            Long orderId,
            Long userId,
            BigDecimal totalAmount,
            String currency,
            String createdAt,
            List<OrderCreatedItem> items
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<OrderCreatedItem> getItems() {
        return items;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setItems(List<OrderCreatedItem> items) {
        this.items = items;
    }

    public static class OrderCreatedItem {
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;

        public OrderCreatedItem() {
        }

        public OrderCreatedItem(Long productId, Integer quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public Long getProductId() {
            return productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }
}