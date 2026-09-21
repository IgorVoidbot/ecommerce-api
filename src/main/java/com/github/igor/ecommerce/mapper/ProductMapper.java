package com.github.igor.ecommerce.mapper;

import com.github.igor.ecommerce.dto.response.ProductResponse;
import com.github.igor.ecommerce.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
