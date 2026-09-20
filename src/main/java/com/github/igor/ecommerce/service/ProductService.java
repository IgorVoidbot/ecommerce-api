package com.github.igor.ecommerce.service;

import com.github.igor.ecommerce.dto.request.ProductRequest;
import com.github.igor.ecommerce.dto.request.ProductUpdateRequest;
import com.github.igor.ecommerce.dto.request.StockUpdateRequest;
import com.github.igor.ecommerce.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    Page<ProductResponse> listAll(String name, Long categoryId, Pageable pageable);

    ProductResponse findById(Long id);

    ProductResponse update(Long id, ProductUpdateRequest request);

    void delete(Long id);

    ProductResponse adjustStock(Long id, StockUpdateRequest request);
}
