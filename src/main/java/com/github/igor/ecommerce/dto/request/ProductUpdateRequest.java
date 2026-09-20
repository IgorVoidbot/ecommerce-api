package com.github.igor.ecommerce.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        String name,
        String description,
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        BigDecimal price,
        @Min(value = 0, message = "Stock quantity must be zero or greater")
        Integer stockQuantity,
        @Positive(message = "Category id must be positive")
        Long categoryId
) {
}
