package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.request.CartItemRequest;
import com.github.igor.ecommerce.dto.request.CartItemUpdateRequest;
import com.github.igor.ecommerce.dto.response.CartItemResponse;
import com.github.igor.ecommerce.dto.response.CartResponse;
import com.github.igor.ecommerce.entity.Cart;
import com.github.igor.ecommerce.entity.CartItem;
import com.github.igor.ecommerce.entity.Product;
import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.enums.Role;
import com.github.igor.ecommerce.exception.BusinessException;
import com.github.igor.ecommerce.exception.NotFoundException;
import com.github.igor.ecommerce.repository.CartItemRepository;
import com.github.igor.ecommerce.repository.CartRepository;
import com.github.igor.ecommerce.repository.ProductRepository;
import com.github.igor.ecommerce.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new User("Igor", "igor@example.com", "hashed-password", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        cart = new Cart(user);
        ReflectionTestUtils.setField(cart, "id", 10L);
    }

    @Test
    void getCartShouldReturnItemsAndCalculatedTotal() {
        givenExistingCart();
        Product springGuide = product(1L, "Spring Boot Guide", "99.90", 10);
        Product dockerHandbook = product(2L, "Docker Handbook", "25.50", 5);
        cart.addItem(cartItem(1L, springGuide, 2));
        cart.addItem(cartItem(2L, dockerHandbook, 3));

        CartResponse response = cartService.getCart();

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("276.30");
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("199.80");
        assertThat(response.items().get(1).subtotal()).isEqualByComparingTo("76.50");
    }

    @Test
    void getCartShouldCreateCartWhenUserHasNone() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart created = invocation.getArgument(0);
            ReflectionTestUtils.setField(created, "id", 10L);
            return created;
        });

        CartResponse response = cartService.getCart();

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.items()).isEmpty();
        assertThat(response.totalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void addItemShouldAddNewItemToCart() {
        givenExistingCart();
        Product product = product(1L, "Spring Boot Guide", "99.90", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 5L);
            return item;
        });

        CartResponse response = cartService.addItem(new CartItemRequest(1L, 2));

        assertThat(response.items()).hasSize(1);
        CartItemResponse item = response.items().get(0);
        assertThat(item.id()).isEqualTo(5L);
        assertThat(item.productId()).isEqualTo(1L);
        assertThat(item.productName()).isEqualTo("Spring Boot Guide");
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.subtotal()).isEqualByComparingTo("199.80");
        assertThat(response.totalAmount()).isEqualByComparingTo("199.80");
    }

    @Test
    void addItemShouldThrowNotFoundExceptionWhenProductDoesNotExist() {
        givenExistingCart();
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(new CartItemRequest(99L, 1)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addItemShouldMergeQuantityWhenProductAlreadyInCart() {
        givenExistingCart();
        Product product = product(1L, "Spring Boot Guide", "99.90", 10);
        cart.addItem(cartItem(1L, product, 2));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        CartResponse response = cartService.addItem(new CartItemRequest(1L, 3));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("499.50");
        assertThat(response.totalAmount()).isEqualByComparingTo("499.50");
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addItemShouldThrowBusinessExceptionWhenQuantityExceedsStock() {
        givenExistingCart();
        Product product = product(1L, "Spring Boot Guide", "99.90", 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(new CartItemRequest(1L, 6)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds available stock");

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addItemShouldThrowBusinessExceptionWhenMergedQuantityExceedsStock() {
        givenExistingCart();
        Product product = product(1L, "Spring Boot Guide", "99.90", 10);
        cart.addItem(cartItem(1L, product, 8));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(new CartItemRequest(1L, 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds available stock");

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(8);
    }

    @Test
    void updateItemQuantityShouldReplaceQuantity() {
        givenExistingCart();
        Product product = product(1L, "Spring Boot Guide", "99.90", 10);
        CartItem item = cartItem(1L, product, 2);
        cart.addItem(item);

        CartResponse response = cartService.updateItemQuantity(1L, new CartItemUpdateRequest(7));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(7);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("699.30");
        assertThat(item.getQuantity()).isEqualTo(7);
    }

    @Test
    void updateItemQuantityShouldThrowBusinessExceptionWhenQuantityExceedsStock() {
        givenExistingCart();
        Product product = product(1L, "Spring Boot Guide", "99.90", 5);
        cart.addItem(cartItem(1L, product, 2));

        assertThatThrownBy(() -> cartService.updateItemQuantity(1L, new CartItemUpdateRequest(6)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds available stock");

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void updateItemQuantityShouldThrowNotFoundExceptionWhenItemNotInCart() {
        givenExistingCart();

        assertThatThrownBy(() -> cartService.updateItemQuantity(99L, new CartItemUpdateRequest(2)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateItemQuantityShouldThrowNotFoundExceptionWhenItemBelongsToAnotherCart() {
        givenExistingCart();
        Cart otherCart = new Cart(user);
        ReflectionTestUtils.setField(otherCart, "id", 20L);
        Product product = product(1L, "Spring Boot Guide", "99.90", 10);
        otherCart.addItem(cartItem(3L, product, 4));

        assertThatThrownBy(() -> cartService.updateItemQuantity(3L, new CartItemUpdateRequest(1)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("3");

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void removeItemShouldRemoveItemFromCart() {
        givenExistingCart();
        Product product = product(1L, "Spring Boot Guide", "99.90", 10);
        cart.addItem(cartItem(1L, product, 2));

        cartService.removeItem(1L);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void removeItemShouldThrowNotFoundExceptionWhenItemNotInCart() {
        givenExistingCart();

        assertThatThrownBy(() -> cartService.removeItem(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void clearCartShouldRemoveAllItems() {
        givenExistingCart();
        cart.addItem(cartItem(1L, product(1L, "Spring Boot Guide", "99.90", 10), 2));
        cart.addItem(cartItem(2L, product(2L, "Docker Handbook", "25.50", 5), 1));

        cartService.clearCart();

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void clearCartShouldCreateCartWhenUserHasNone() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart created = invocation.getArgument(0);
            ReflectionTestUtils.setField(created, "id", 10L);
            return created;
        });

        cartService.clearCart();

        verify(cartRepository).save(any(Cart.class));
    }

    private void givenExistingCart() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    }

    private Product product(Long id, String name, String price, Integer stock) {
        Product product = new Product(name, "A book", new BigDecimal(price), stock, null);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private CartItem cartItem(Long id, Product product, Integer quantity) {
        CartItem item = new CartItem(cart, product, quantity);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
