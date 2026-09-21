package com.github.igor.ecommerce.service;

import com.github.igor.ecommerce.dto.request.CartItemRequest;
import com.github.igor.ecommerce.dto.request.CartItemUpdateRequest;
import com.github.igor.ecommerce.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart();

    CartResponse addItem(CartItemRequest request);

    CartResponse updateItemQuantity(Long itemId, CartItemUpdateRequest request);

    void removeItem(Long itemId);

    void clearCart();
}
