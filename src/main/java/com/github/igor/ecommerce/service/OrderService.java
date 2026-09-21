package com.github.igor.ecommerce.service;

import com.github.igor.ecommerce.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse checkout();

    Page<OrderResponse> listMyOrders(Pageable pageable);

    OrderResponse findMyOrderById(Long id);

    Page<OrderResponse> listAllOrders(Pageable pageable);
}
