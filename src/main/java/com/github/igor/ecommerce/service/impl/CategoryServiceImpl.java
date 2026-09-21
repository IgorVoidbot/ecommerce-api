package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.request.CategoryRequest;
import com.github.igor.ecommerce.dto.response.CategoryResponse;
import com.github.igor.ecommerce.entity.Category;
import com.github.igor.ecommerce.exception.ConflictException;
import com.github.igor.ecommerce.exception.NotFoundException;
import com.github.igor.ecommerce.mapper.CategoryMapper;
import com.github.igor.ecommerce.repository.CategoryRepository;
import com.github.igor.ecommerce.repository.ProductRepository;
import com.github.igor.ecommerce.service.CategoryService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ConflictException("Category already exists with name: " + request.name());
        }
        Category category = new Category(request.name());
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findEntityById(id);
        if (categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ConflictException("Category already exists with name: " + request.name());
        }
        category.setName(request.name());
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findEntityById(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Cannot delete category with id " + id + " because it has linked products");
        }
        categoryRepository.delete(category);
    }

    private Category findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
    }
}
