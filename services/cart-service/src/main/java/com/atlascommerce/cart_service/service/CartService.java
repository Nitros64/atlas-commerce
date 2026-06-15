package com.atlascommerce.cart_service.service;

import com.atlascommerce.cart_service.dto.AddCartItemRequest;
import com.atlascommerce.cart_service.dto.CartResponse;
import com.atlascommerce.cart_service.dto.UpdateCartItemRequest;
import com.atlascommerce.cart_service.entity.Cart;
import com.atlascommerce.cart_service.entity.CartItem;
import com.atlascommerce.cart_service.exception.CartItemNotFoundException;
import com.atlascommerce.cart_service.exception.CartNotFoundException;
import com.atlascommerce.cart_service.mapper.CartMapper;
import com.atlascommerce.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository repository;
    private final CartMapper mapper;

    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);

        return mapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.productId()))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.setQuantity(item.getQuantity() + request.quantity()),
                        () -> cart.getItems().add(
                                CartItem.builder()
                                        .productId(request.productId())
                                        .quantity(request.quantity())
                                        .unitPrice(request.unitPrice())
                                        .cart(cart)
                                        .build()
                        )
                );

        return mapper.toResponse(repository.save(cart));
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        item.setQuantity(request.quantity());

        return mapper.toResponse(repository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);

        boolean removed = cart.getItems()
                .removeIf(item -> item.getProductId().equals(productId));

        if (!removed) {
            throw new CartItemNotFoundException(productId);
        }

        return mapper.toResponse(repository.save(cart));
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();

        return mapper.toResponse(repository.save(cart));
    }

    private Cart getOrCreateCart(Long userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> repository.save(
                        Cart.builder()
                                .userId(userId)
                                .build()
                ));
    }
}