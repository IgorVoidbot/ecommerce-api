package com.github.igor.ecommerce.mapper;

import com.github.igor.ecommerce.dto.response.CategoryResponse;
import com.github.igor.ecommerce.entity.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
