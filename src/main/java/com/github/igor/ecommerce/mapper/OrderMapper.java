package com.github.igor.ecommerce.mapper;

import com.github.igor.ecommerce.dto.response.OrderItemResponse;
import com.github.igor.ecommerce.dto.response.OrderResponse;
import com.github.igor.ecommerce.entity.Order;
import com.github.igor.ecommerce.entity.OrderItem;

import java.util.Comparator;
import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(OrderMapper::toItemResponse)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items
        );
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
