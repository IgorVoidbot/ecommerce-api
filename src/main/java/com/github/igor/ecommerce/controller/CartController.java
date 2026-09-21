package com.github.igor.ecommerce.controller;

import com.github.igor.ecommerce.dto.request.CartItemRequest;
import com.github.igor.ecommerce.dto.request.CartItemUpdateRequest;
import com.github.igor.ecommerce.dto.response.CartResponse;
import com.github.igor.ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@PathVariable Long itemId,
                                                           @Valid @RequestBody CartItemUpdateRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {
        cartService.removeItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}
