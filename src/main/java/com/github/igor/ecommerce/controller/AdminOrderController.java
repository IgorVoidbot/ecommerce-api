package com.github.igor.ecommerce.controller;

import com.github.igor.ecommerce.dto.request.OrderStatusUpdateRequest;
import com.github.igor.ecommerce.dto.response.OrderResponse;
import com.github.igor.ecommerce.dto.response.PageResponse;
import com.github.igor.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> listAllOrders(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(orderService.listAllOrders(pageable)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }
}
