package com.github.igor.ecommerce.mapper;

import com.github.igor.ecommerce.dto.response.CartItemResponse;
import com.github.igor.ecommerce.dto.response.CartResponse;
import com.github.igor.ecommerce.entity.Cart;
import com.github.igor.ecommerce.entity.CartItem;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class CartMapper {

    private CartMapper() {
    }

    public static CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(CartMapper::toItemResponse)
                .toList();
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, totalAmount);
    }

    public static CartItemResponse toItemResponse(CartItem item) {
        BigDecimal unitPrice = item.getProduct().getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                unitPrice,
                item.getQuantity(),
                subtotal
        );
    }
}
