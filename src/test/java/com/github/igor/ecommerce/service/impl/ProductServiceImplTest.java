package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.request.ProductRequest;
import com.github.igor.ecommerce.dto.request.ProductUpdateRequest;
import com.github.igor.ecommerce.dto.request.StockUpdateRequest;
import com.github.igor.ecommerce.dto.response.ProductResponse;
import com.github.igor.ecommerce.entity.Category;
import com.github.igor.ecommerce.entity.Product;
import com.github.igor.ecommerce.exception.BusinessException;
import com.github.igor.ecommerce.exception.NotFoundException;
import com.github.igor.ecommerce.repository.CategoryRepository;
import com.github.igor.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createShouldReturnProductResponseWhenCategoryExists() {
        ProductRequest request = new ProductRequest("Spring Boot Guide", "A book", new BigDecimal("99.90"), 10, 1L);
        Category category = category(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", 10L);
            return product;
        });

        ProductResponse response = productService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Spring Boot Guide");
        assertThat(response.categoryId()).isEqualTo(1L);
        assertThat(response.categoryName()).isEqualTo("Programação");
        assertThat(response.stockQuantity()).isEqualTo(10);
    }

    @Test
    void createShouldThrowNotFoundExceptionWhenCategoryDoesNotExist() {
        ProductRequest request = new ProductRequest("Spring Boot Guide", "A book", new BigDecimal("99.90"), 10, 99L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void findByIdShouldReturnProductResponseWhenFound() {
        Product product = product(1L, category(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Spring Boot Guide");
        assertThat(response.categoryId()).isEqualTo(1L);
    }

    @Test
    void findByIdShouldThrowNotFoundExceptionWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listAllShouldReturnPageWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product(1L, category(1L))), pageable, 1);
        when(productRepository.search(eq("%%"), isNull(), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = productService.listAll(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    void listAllShouldFilterByNameIgnoringCase() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product(1L, category(1L))), pageable, 1);
        when(productRepository.search(eq("%spring%"), isNull(), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = productService.listAll("Spring", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).search("%spring%", null, pageable);
    }

    @Test
    void listAllShouldFilterByCategoryId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product(1L, category(1L))), pageable, 1);
        when(productRepository.search(eq("%%"), eq(1L), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = productService.listAll(null, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).search("%%", 1L, pageable);
    }

    @Test
    void listAllShouldCombineNameAndCategoryFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product(1L, category(1L))), pageable, 1);
        when(productRepository.search(eq("%spring%"), eq(1L), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = productService.listAll("spring", 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).search("%spring%", 1L, pageable);
    }

    @Test
    void listAllShouldTreatBlankNameAsNoFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(), pageable, 0);
        when(productRepository.search(eq("%%"), isNull(), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = productService.listAll("   ", null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(productRepository).search("%%", null, pageable);
    }

    @Test
    void updateShouldApplyOnlyProvidedFields() {
        ProductUpdateRequest request = new ProductUpdateRequest("New Name", null, null, null, null);
        Product product = product(1L, category(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.update(1L, request);

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.price()).isEqualByComparingTo("99.90");
        assertThat(response.categoryId()).isEqualTo(1L);
    }

    @Test
    void updateShouldChangeCategoryWhenProvided() {
        ProductUpdateRequest request = new ProductUpdateRequest(null, null, null, null, 2L);
        Product product = product(1L, category(1L));
        Category newCategory = category(2L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));

        ProductResponse response = productService.update(1L, request);

        assertThat(response.categoryId()).isEqualTo(2L);
        assertThat(response.categoryName()).isEqualTo("Programação");
    }

    @Test
    void updateShouldThrowNotFoundExceptionWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, new ProductUpdateRequest(null, null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateShouldThrowNotFoundExceptionWhenNewCategoryDoesNotExist() {
        ProductUpdateRequest request = new ProductUpdateRequest(null, null, null, null, 99L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, category(1L))));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(1L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteShouldRemoveProduct() {
        Product product = product(1L, category(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteShouldThrowNotFoundExceptionWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void adjustStockShouldIncreaseQuantity() {
        StockUpdateRequest request = new StockUpdateRequest(5);
        Product product = product(1L, category(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.adjustStock(1L, request);

        assertThat(response.stockQuantity()).isEqualTo(15);
    }

    @Test
    void adjustStockShouldDecreaseQuantity() {
        StockUpdateRequest request = new StockUpdateRequest(-4);
        Product product = product(1L, category(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.adjustStock(1L, request);

        assertThat(response.stockQuantity()).isEqualTo(6);
    }

    @Test
    void adjustStockShouldThrowBusinessExceptionWhenStockWouldBeNegative() {
        StockUpdateRequest request = new StockUpdateRequest(-11);
        Product product = product(1L, category(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.adjustStock(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");

        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void adjustStockShouldThrowNotFoundExceptionWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.adjustStock(99L, new StockUpdateRequest(5)))
                .isInstanceOf(NotFoundException.class);
    }

    private Category category(Long id) {
        Category category = new Category("Programação");
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Product product(Long id, Category category) {
        Product product = new Product("Spring Boot Guide", "A book", new BigDecimal("99.90"), 10, category);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
