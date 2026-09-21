package com.github.igor.ecommerce.dto.request;

import com.github.igor.ecommerce.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
