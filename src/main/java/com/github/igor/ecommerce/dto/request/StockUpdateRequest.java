package com.github.igor.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;

public record StockUpdateRequest(
        @NotNull(message = "Quantity is required")
        Integer quantity
) {
}
