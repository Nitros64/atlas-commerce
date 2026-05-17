package com.atlascommerce.cart_service.mapper;

import com.atlascommerce.cart_service.dto.CartItemResponse;
import com.atlascommerce.cart_service.dto.CartResponse;
import com.atlascommerce.cart_service.entity.Cart;
import com.atlascommerce.cart_service.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                items,
                total,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal subtotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                subtotal
        );
    }
}