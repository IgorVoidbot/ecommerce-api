package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.request.CartItemRequest;
import com.github.igor.ecommerce.dto.request.CartItemUpdateRequest;
import com.github.igor.ecommerce.dto.response.CartResponse;
import com.github.igor.ecommerce.entity.Cart;
import com.github.igor.ecommerce.entity.CartItem;
import com.github.igor.ecommerce.entity.Product;
import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.exception.BusinessException;
import com.github.igor.ecommerce.exception.NotFoundException;
import com.github.igor.ecommerce.mapper.CartMapper;
import com.github.igor.ecommerce.repository.CartItemRepository;
import com.github.igor.ecommerce.repository.CartRepository;
import com.github.igor.ecommerce.repository.ProductRepository;
import com.github.igor.ecommerce.security.CurrentUserService;
import com.github.igor.ecommerce.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           CurrentUserService currentUserService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional
    public CartResponse getCart() {
        return CartMapper.toResponse(getOrCreateCart());
    }

    @Override
    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        Cart cart = getOrCreateCart();
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.productId()));
        CartItem existing = cart.findItemByProductId(request.productId()).orElse(null);
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        validateStock(product, newQuantity);
        if (existing == null) {
            CartItem item = cartItemRepository.save(new CartItem(cart, product, request.quantity()));
            cart.addItem(item);
        } else {
            existing.setQuantity(newQuantity);
        }
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long itemId, CartItemUpdateRequest request) {
        Cart cart = getOrCreateCart();
        CartItem item = cart.findItemById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + itemId));
        validateStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public void removeItem(Long itemId) {
        Cart cart = getOrCreateCart();
        CartItem item = cart.findItemById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + itemId));
        cart.removeItem(item);
    }

    @Override
    @Transactional
    public void clearCart() {
        getOrCreateCart().clearItems();
    }

    private Cart getOrCreateCart() {
        User user = currentUserService.getCurrentUser();
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    private void validateStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new BusinessException("Requested quantity " + quantity
                    + " exceeds available stock " + product.getStockQuantity()
                    + " for product id " + product.getId());
        }
    }
}
