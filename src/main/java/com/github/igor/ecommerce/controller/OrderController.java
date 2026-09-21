package com.github.igor.ecommerce.controller;

import com.github.igor.ecommerce.dto.response.OrderResponse;
import com.github.igor.ecommerce.dto.response.PageResponse;
import com.github.igor.ecommerce.service.OrderService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> checkout() {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout());
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> listMyOrders(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(orderService.listMyOrders(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findMyOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findMyOrderById(id));
    }
}
