package com.github.igor.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
