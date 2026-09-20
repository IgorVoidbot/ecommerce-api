package com.github.igor.ecommerce.service;

import com.github.igor.ecommerce.dto.request.CategoryRequest;
import com.github.igor.ecommerce.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    List<CategoryResponse> listAll();

    CategoryResponse findById(Long id);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}
