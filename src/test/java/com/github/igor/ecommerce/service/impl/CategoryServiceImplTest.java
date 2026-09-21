package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.request.CategoryRequest;
import com.github.igor.ecommerce.dto.response.CategoryResponse;
import com.github.igor.ecommerce.entity.Category;
import com.github.igor.ecommerce.exception.ConflictException;
import com.github.igor.ecommerce.exception.NotFoundException;
import com.github.igor.ecommerce.repository.CategoryRepository;
import com.github.igor.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void createShouldReturnCategoryResponseWhenNameIsAvailable() {
        CategoryRequest request = new CategoryRequest("Programação");
        when(categoryRepository.existsByName("Programação")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            ReflectionTestUtils.setField(category, "id", 1L);
            return category;
        });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Programação");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createShouldThrowConflictExceptionWhenNameAlreadyExists() {
        CategoryRequest request = new CategoryRequest("Programação");
        when(categoryRepository.existsByName("Programação")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Programação");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void findByIdShouldReturnCategoryResponseWhenFound() {
        Category category = new Category("Mobile");
        ReflectionTestUtils.setField(category, "id", 1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Mobile");
    }

    @Test
    void findByIdShouldThrowNotFoundExceptionWhenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listAllShouldReturnAllCategoriesOrderedByName() {
        Category first = new Category("Business");
        Category second = new Category("Mobile");
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        when(categoryRepository.findAll(Sort.by("name"))).thenReturn(List.of(first, second));

        List<CategoryResponse> responses = categoryService.listAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Business");
        assertThat(responses.get(1).name()).isEqualTo("Mobile");
    }

    @Test
    void updateShouldRenameCategoryWhenNameIsAvailable() {
        CategoryRequest request = new CategoryRequest("Back-end");
        Category category = new Category("Programação");
        ReflectionTestUtils.setField(category, "id", 1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Back-end", 1L)).thenReturn(false);

        CategoryResponse response = categoryService.update(1L, request);

        assertThat(response.name()).isEqualTo("Back-end");
        assertThat(category.getName()).isEqualTo("Back-end");
    }

    @Test
    void updateShouldThrowConflictExceptionWhenNameBelongsToAnotherCategory() {
        CategoryRequest request = new CategoryRequest("Mobile");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category("Programação")));
        when(categoryRepository.existsByNameAndIdNot("Mobile", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Mobile");
    }

    @Test
    void updateShouldThrowNotFoundExceptionWhenCategoryNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, new CategoryRequest("Mobile")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteShouldRemoveCategoryWithoutProducts() {
        Category category = new Category("Programação");
        ReflectionTestUtils.setField(category, "id", 1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteShouldThrowConflictExceptionWhenCategoryHasProducts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category("Programação")));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteShouldThrowNotFoundExceptionWhenCategoryNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(NotFoundException.class);
    }
}
