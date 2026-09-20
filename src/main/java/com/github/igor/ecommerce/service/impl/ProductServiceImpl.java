package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.request.ProductRequest;
import com.github.igor.ecommerce.dto.request.ProductUpdateRequest;
import com.github.igor.ecommerce.dto.request.StockUpdateRequest;
import com.github.igor.ecommerce.dto.response.ProductResponse;
import com.github.igor.ecommerce.entity.Category;
import com.github.igor.ecommerce.entity.Product;
import com.github.igor.ecommerce.exception.BusinessException;
import com.github.igor.ecommerce.exception.NotFoundException;
import com.github.igor.ecommerce.mapper.ProductMapper;
import com.github.igor.ecommerce.repository.CategoryRepository;
import com.github.igor.ecommerce.repository.ProductRepository;
import com.github.igor.ecommerce.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + request.categoryId()));
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity(),
                category
        );
        return ProductMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listAll(String name, Long categoryId, Pageable pageable) {
        String pattern = (name == null || name.isBlank())
                ? "%%"
                : "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
        return productRepository.search(pattern, categoryId, pageable)
                .map(ProductMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = findEntityById(id);
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.stockQuantity() != null) {
            product.setStockQuantity(request.stockQuantity());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found with id: " + request.categoryId()));
            product.setCategory(category);
        }
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findEntityById(id);
        productRepository.delete(product);
    }

    @Override
    @Transactional
    public ProductResponse adjustStock(Long id, StockUpdateRequest request) {
        Product product = findEntityById(id);
        int newQuantity = product.getStockQuantity() + request.quantity();
        if (newQuantity < 0) {
            throw new BusinessException(
                    "Insufficient stock for product id " + id + ": current stock is " + product.getStockQuantity()
            );
        }
        product.setStockQuantity(newQuantity);
        return ProductMapper.toResponse(product);
    }

    private Product findEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }
}
