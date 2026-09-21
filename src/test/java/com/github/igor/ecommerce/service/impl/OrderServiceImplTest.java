package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.request.OrderStatusUpdateRequest;
import com.github.igor.ecommerce.dto.response.OrderItemResponse;
import com.github.igor.ecommerce.dto.response.OrderResponse;
import com.github.igor.ecommerce.entity.Cart;
import com.github.igor.ecommerce.entity.CartItem;
import com.github.igor.ecommerce.entity.Order;
import com.github.igor.ecommerce.entity.OrderItem;
import com.github.igor.ecommerce.entity.Product;
import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.enums.OrderStatus;
import com.github.igor.ecommerce.enums.Role;
import com.github.igor.ecommerce.exception.BusinessException;
import com.github.igor.ecommerce.exception.ForbiddenException;
import com.github.igor.ecommerce.exception.NotFoundException;
import com.github.igor.ecommerce.repository.CartRepository;
import com.github.igor.ecommerce.repository.OrderRepository;
import com.github.igor.ecommerce.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private OrderServiceImpl orderService;

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
    void checkoutShouldCreatePendingOrderWithSnapshotsAndClearCart() {
        givenExistingCart();
        Product springGuide = product(1L, "Spring Boot Guide", "99.90", 10);
        Product dockerHandbook = product(2L, "Docker Handbook", "25.50", 5);
        cart.addItem(cartItem(1L, springGuide, 2));
        cart.addItem(cartItem(2L, dockerHandbook, 3));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 100L);
            return order;
        });

        OrderResponse response = orderService.checkout();

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.userName()).isEqualTo("Igor");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("276.30");
        assertThat(response.items()).hasSize(2);

        OrderItemResponse first = response.items().get(0);
        assertThat(first.productId()).isEqualTo(1L);
        assertThat(first.productName()).isEqualTo("Spring Boot Guide");
        assertThat(first.unitPrice()).isEqualByComparingTo("99.90");
        assertThat(first.quantity()).isEqualTo(2);
        assertThat(first.subtotal()).isEqualByComparingTo("199.80");

        OrderItemResponse second = response.items().get(1);
        assertThat(second.productId()).isEqualTo(2L);
        assertThat(second.productName()).isEqualTo("Docker Handbook");
        assertThat(second.unitPrice()).isEqualByComparingTo("25.50");
        assertThat(second.quantity()).isEqualTo(3);
        assertThat(second.subtotal()).isEqualByComparingTo("76.50");

        assertThat(springGuide.getStockQuantity()).isEqualTo(8);
        assertThat(dockerHandbook.getStockQuantity()).isEqualTo(2);
        assertThat(cart.getItems()).isEmpty();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void checkoutShouldThrowBusinessExceptionWhenCartDoesNotExist() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.checkout())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty cart");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkoutShouldThrowBusinessExceptionWhenCartIsEmpty() {
        givenExistingCart();

        assertThatThrownBy(() -> orderService.checkout())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty cart");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkoutShouldThrowBusinessExceptionWhenStockInsufficientWithoutPartialEffects() {
        givenExistingCart();
        Product springGuide = product(1L, "Spring Boot Guide", "99.90", 10);
        Product dockerHandbook = product(2L, "Docker Handbook", "25.50", 5);
        cart.addItem(cartItem(1L, springGuide, 2));
        cart.addItem(cartItem(2L, dockerHandbook, 6));

        assertThatThrownBy(() -> orderService.checkout())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds available stock");

        assertThat(springGuide.getStockQuantity()).isEqualTo(10);
        assertThat(dockerHandbook.getStockQuantity()).isEqualTo(5);
        assertThat(cart.getItems()).hasSize(2);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void listMyOrdersShouldReturnPaginatedOrders() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        Order order = order(100L, user, OrderStatus.PAID);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findByUserId(1L, pageable)).thenReturn(page);

        Page<OrderResponse> result = orderService.listMyOrders(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(100L);
        assertThat(result.getContent().get(0).status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void listAllOrdersShouldReturnPaginatedOrders() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        Order order = order(100L, user, OrderStatus.PENDING);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findAll(pageable)).thenReturn(page);

        Page<OrderResponse> result = orderService.listAllOrders(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(100L);
    }

    @Test
    void findMyOrderByIdShouldReturnOrderWhenOwner() {
        Order order = order(100L, user, OrderStatus.PAID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.findMyOrderById(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void findMyOrderByIdShouldThrowForbiddenExceptionWhenOrderBelongsToAnotherUser() {
        User otherUser = new User("Ana", "ana@example.com", "hashed-password", Role.USER);
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        Order order = order(100L, otherUser, OrderStatus.PAID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.findMyOrderById(100L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void findMyOrderByIdShouldAllowAdminToAccessAnotherUserOrder() {
        User otherUser = new User("Ana", "ana@example.com", "hashed-password", Role.USER);
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        User admin = new User("Admin", "admin@ecommerce.com", "hashed-password", Role.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 3L);
        Order order = order(100L, otherUser, OrderStatus.PAID);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.findMyOrderById(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(2L);
    }

    @Test
    void findMyOrderByIdShouldThrowNotFoundExceptionWhenOrderDoesNotExist() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findMyOrderById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateStatusShouldAdvanceThroughValidTransitions() {
        Order order = order(100L, user, OrderStatus.PENDING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderResponse paid = orderService.updateStatus(100L, new OrderStatusUpdateRequest(OrderStatus.PAID));
        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);

        OrderResponse shipped = orderService.updateStatus(100L, new OrderStatusUpdateRequest(OrderStatus.SHIPPED));
        assertThat(shipped.status()).isEqualTo(OrderStatus.SHIPPED);

        OrderResponse delivered = orderService.updateStatus(100L, new OrderStatusUpdateRequest(OrderStatus.DELIVERED));
        assertThat(delivered.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void updateStatusShouldThrowBusinessExceptionOnInvalidTransition() {
        Order order = order(100L, user, OrderStatus.SHIPPED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, new OrderStatusUpdateRequest(OrderStatus.PENDING)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void updateStatusShouldThrowBusinessExceptionWhenSkippingSteps() {
        Order order = order(100L, user, OrderStatus.PENDING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, new OrderStatusUpdateRequest(OrderStatus.SHIPPED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void updateStatusShouldThrowBusinessExceptionWhenCancellingDeliveredOrder() {
        Order order = order(100L, user, OrderStatus.DELIVERED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, new OrderStatusUpdateRequest(OrderStatus.CANCELLED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateStatusToCancelledShouldReturnStockToProducts() {
        Product product = product(1L, "Spring Boot Guide", "99.90", 8);
        Order order = order(100L, user, OrderStatus.PENDING);
        order.addItem(orderItem(1L, order, product, 2));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.updateStatus(100L, new OrderStatusUpdateRequest(OrderStatus.CANCELLED));

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void updateStatusShouldBeIdempotentWhenOrderAlreadyCancelled() {
        Product product = product(1L, "Spring Boot Guide", "99.90", 10);
        Order order = order(100L, user, OrderStatus.CANCELLED);
        order.addItem(orderItem(1L, order, product, 2));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, new OrderStatusUpdateRequest(OrderStatus.CANCELLED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");

        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void updateStatusShouldThrowBusinessExceptionOnSameStatusTransition() {
        Order order = order(100L, user, OrderStatus.PAID);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, new OrderStatusUpdateRequest(OrderStatus.PAID)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateStatusShouldThrowNotFoundExceptionWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(99L, new OrderStatusUpdateRequest(OrderStatus.PAID)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
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

    private Order order(Long id, User owner, OrderStatus status) {
        Order order = new Order(owner);
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "status", status);
        order.setTotalAmount(BigDecimal.ZERO);
        return order;
    }

    private OrderItem orderItem(Long id, Order order, Product product, Integer quantity) {
        OrderItem item = new OrderItem(order, product, product.getName(), product.getPrice(), quantity);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
