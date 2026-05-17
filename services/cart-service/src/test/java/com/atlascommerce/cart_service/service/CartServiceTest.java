package com.atlascommerce.cart_service.service;

import com.atlascommerce.cart_service.dto.AddCartItemRequest;
import com.atlascommerce.cart_service.dto.CartResponse;
import com.atlascommerce.cart_service.dto.UpdateCartItemRequest;
import com.atlascommerce.cart_service.entity.Cart;
import com.atlascommerce.cart_service.entity.CartItem;
import com.atlascommerce.cart_service.exception.CartItemNotFoundException;
import com.atlascommerce.cart_service.mapper.CartMapper;
import com.atlascommerce.cart_service.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository repository;

    @Mock
    private CartMapper mapper;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private CartResponse response;

    @BeforeEach
    void setUp() {
        cart = Cart.builder()
                .id(1L)
                .userId(1L)
                .items(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        response = new CartResponse(
                1L,
                1L,
                java.util.List.of(),
                BigDecimal.ZERO,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    @Test
    void getCart_shouldReturnExistingCart() {
        when(repository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(mapper.toResponse(cart)).thenReturn(response);

        CartResponse result = cartService.getCart(1L);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(1L);

        verify(repository, never()).save(any());
    }

    @Test
    void getCart_shouldCreateCart_whenCartDoesNotExist() {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(Cart.class))).thenReturn(cart);
        when(mapper.toResponse(cart)).thenReturn(response);

        CartResponse result = cartService.getCart(1L);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(1L);

        verify(repository).save(argThat(saved -> saved.getUserId().equals(1L)));
    }

    @Test
    void addItem_shouldAddNewItem_whenProductDoesNotExistInCart() {
        AddCartItemRequest request = new AddCartItemRequest(
                101L,
                2,
                new BigDecimal("19.99")
        );

        when(repository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(repository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Cart.class))).thenReturn(response);

        cartService.addItem(1L, request);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProductId()).isEqualTo(101L);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getItems().get(0).getUnitPrice()).isEqualByComparingTo("19.99");
        assertThat(cart.getItems().get(0).getCart()).isEqualTo(cart);

        verify(repository).save(cart);
    }

    @Test
    void addItem_shouldIncreaseQuantity_whenProductAlreadyExistsInCart() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .productId(101L)
                .quantity(2)
                .unitPrice(new BigDecimal("19.99"))
                .cart(cart)
                .build();

        cart.getItems().add(existingItem);

        AddCartItemRequest request = new AddCartItemRequest(
                101L,
                3,
                new BigDecimal("19.99")
        );

        when(repository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(repository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Cart.class))).thenReturn(response);

        cartService.addItem(1L, request);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(existingItem.getQuantity()).isEqualTo(5);

        verify(repository).save(cart);
    }

    @Test
    void updateItem_shouldUpdateQuantitySuccessfully() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .productId(101L)
                .quantity(2)
                .unitPrice(new BigDecimal("19.99"))
                .cart(cart)
                .build();

        cart.getItems().add(existingItem);

        UpdateCartItemRequest request = new UpdateCartItemRequest(7);

        when(repository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(repository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Cart.class))).thenReturn(response);

        cartService.updateItem(1L, 101L, request);

        assertThat(existingItem.getQuantity()).isEqualTo(7);

        verify(repository).save(cart);
    }

    @Test
    void updateItem_shouldThrowException_whenItemDoesNotExist() {
        UpdateCartItemRequest request = new UpdateCartItemRequest(7);

        when(repository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem(1L, 999L, request))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).save(any());
    }

    @Test
    void removeItem_shouldRemoveItemSuccessfully() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .productId(101L)
                .quantity(2)
                .unitPrice(new BigDecimal("19.99"))
                .cart(cart)
                .build();

        cart.getItems().add(existingItem);

        when(repository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(repository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Cart.class))).thenReturn(response);

        cartService.removeItem(1L, 101L);

        assertThat(cart.getItems()).isEmpty();

        verify(repository).save(cart);
    }

    @Test
    void removeItem_shouldThrowException_whenItemDoesNotExist() {
        when(repository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem(1L, 999L))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).save(any());
    }

    @Test
    void clearCart_shouldRemoveAllItemsSuccessfully() {
        cart.getItems().add(
                CartItem.builder()
                        .id(1L)
                        .productId(101L)
                        .quantity(2)
                        .unitPrice(new BigDecimal("19.99"))
                        .cart(cart)
                        .build()
        );

        when(repository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(repository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Cart.class))).thenReturn(response);

        cartService.clearCart(1L);

        assertThat(cart.getItems()).isEmpty();

        verify(repository).save(cart);
    }
}