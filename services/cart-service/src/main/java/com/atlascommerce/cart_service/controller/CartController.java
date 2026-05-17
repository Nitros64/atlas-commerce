package com.atlascommerce.cart_service.controller;

import com.atlascommerce.cart_service.dto.AddCartItemRequest;
import com.atlascommerce.cart_service.dto.CartResponse;
import com.atlascommerce.cart_service.dto.UpdateCartItemRequest;
import com.atlascommerce.cart_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CartResponse getCart(@PathVariable Long userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/{userId}/items")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CartResponse addItem(
            @PathVariable Long userId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(userId, request);
    }

    @PutMapping("/{userId}/items/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CartResponse updateItem(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(userId, productId, request);
    }

    @DeleteMapping("/{userId}/items/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CartResponse removeItem(
            @PathVariable Long userId,
            @PathVariable Long productId
    ) {
        return cartService.removeItem(userId, productId);
    }

    @DeleteMapping("/{userId}/clear")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CartResponse clearCart(@PathVariable Long userId) {
        return cartService.clearCart(userId);
    }
}