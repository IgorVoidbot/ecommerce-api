package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.response.OrderResponse;
import com.github.igor.ecommerce.entity.Cart;
import com.github.igor.ecommerce.entity.CartItem;
import com.github.igor.ecommerce.entity.Order;
import com.github.igor.ecommerce.entity.OrderItem;
import com.github.igor.ecommerce.entity.Product;
import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.enums.Role;
import com.github.igor.ecommerce.exception.BusinessException;
import com.github.igor.ecommerce.exception.ForbiddenException;
import com.github.igor.ecommerce.exception.NotFoundException;
import com.github.igor.ecommerce.mapper.OrderMapper;
import com.github.igor.ecommerce.repository.CartRepository;
import com.github.igor.ecommerce.repository.OrderRepository;
import com.github.igor.ecommerce.security.CurrentUserService;
import com.github.igor.ecommerce.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CurrentUserService currentUserService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartRepository cartRepository,
                            CurrentUserService currentUserService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional
    public OrderResponse checkout() {
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Cannot checkout an empty cart"));
        List<CartItem> cartItems = List.copyOf(cart.getItems());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Cannot checkout an empty cart");
        }
        validateStockForAllItems(cartItems);
        Order order = new Order(user);
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            OrderItem item = new OrderItem(order, product, product.getName(), product.getPrice(), cartItem.getQuantity());
            order.addItem(item);
            totalAmount = totalAmount.add(item.getSubtotal());
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
        }
        order.setTotalAmount(totalAmount);
        Order saved = orderRepository.save(order);
        cart.clearItems();
        return OrderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listMyOrders(Pageable pageable) {
        User user = currentUserService.getCurrentUser();
        return orderRepository.findByUserId(user.getId(), pageable)
                .map(OrderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findMyOrderById(Long id) {
        User user = currentUserService.getCurrentUser();
        Order order = findEntityById(id);
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not allowed to access this order");
        }
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(OrderMapper::toResponse);
    }

    private void validateStockForAllItems(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new BusinessException("Requested quantity " + cartItem.getQuantity()
                        + " exceeds available stock " + product.getStockQuantity()
                        + " for product id " + product.getId());
            }
        }
    }

    private Order findEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }
}
