package com.github.igor.ecommerce.controller;

import com.github.igor.ecommerce.entity.Category;
import com.github.igor.ecommerce.entity.Product;
import com.github.igor.ecommerce.repository.CategoryRepository;
import com.github.igor.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryControllerIntegrationTest {

    private static final String BASE_PATH = "/api/v1/categories";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createShouldReturn201WithIdAndName() throws Exception {
        performPost("{\"name\":\"Data Science\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Data Science"));
    }

    @Test
    void createShouldReturn409WhenNameAlreadyExists() throws Exception {
        expectErrorJson(performPost("{\"name\":\"Mobile\"}"), 409, BASE_PATH);
    }

    @Test
    void createShouldReturn400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value(BASE_PATH))
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
    }

    @Test
    void listAllShouldReturn200WithSeededCategories() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder(
                        "Programação", "Mobile", "Front-end", "Infraestrutura", "Business", "Design e UX")));
    }

    @Test
    void findByIdShouldReturn200WhenCategoryExists() throws Exception {
        Long id = categoryIdByName("Mobile");

        mockMvc.perform(get(BASE_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Mobile"));
    }

    @Test
    void findByIdShouldReturn404WhenCategoryDoesNotExist() throws Exception {
        expectErrorJson(mockMvc.perform(get(BASE_PATH + "/{id}", 999_999L)),
                404, BASE_PATH + "/999999");
    }

    @Test
    void updateShouldReturn200WithNewName() throws Exception {
        Category category = saveCategory("DevOps");

        mockMvc.perform(put(BASE_PATH + "/{id}", category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"SRE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(category.getId()))
                .andExpect(jsonPath("$.name").value("SRE"));

        assertThat(categoryRepository.findById(category.getId()))
                .hasValueSatisfying(persisted -> assertThat(persisted.getName()).isEqualTo("SRE"));
    }

    @Test
    void updateShouldReturn409WhenNewNameBelongsToAnotherCategory() throws Exception {
        Long programacaoId = categoryIdByName("Programação");

        expectErrorJson(mockMvc.perform(put(BASE_PATH + "/{id}", programacaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mobile\"}")),
                409, BASE_PATH + "/" + programacaoId);
    }

    @Test
    void updateShouldReturn404WhenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(put(BASE_PATH + "/{id}", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nova\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/999999"));
    }

    @Test
    void deleteShouldReturn204AndRemoveCategory() throws Exception {
        Category category = saveCategory("Temporária");

        mockMvc.perform(delete(BASE_PATH + "/{id}", category.getId()))
                .andExpect(status().isNoContent());

        assertThat(categoryRepository.findById(category.getId())).isEmpty();
    }

    @Test
    void deleteShouldReturn409WhenCategoryHasLinkedProducts() throws Exception {
        Long programacaoId = categoryIdByName("Programação");
        Category category = categoryRepository.findById(programacaoId).orElseThrow();
        productRepository.save(new Product("Curso de Java", "desc", new BigDecimal("99.90"), 5, category));

        expectErrorJson(mockMvc.perform(delete(BASE_PATH + "/{id}", programacaoId)),
                409, BASE_PATH + "/" + programacaoId);
    }

    @Test
    void deleteShouldReturn404WhenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/999999"));
    }

    private ResultActions performPost(String body) throws Exception {
        return mockMvc.perform(post(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void expectErrorJson(ResultActions actions, int expectedStatus, String expectedPath) throws Exception {
        actions.andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(expectedPath));
    }

    private Long categoryIdByName(String name) {
        return categoryEntityByName(name).getId();
    }

    private Category categoryEntityByName(String name) {
        return categoryRepository.findAll().stream()
                .filter(category -> category.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private Category saveCategory(String name) {
        return categoryRepository.save(new Category(name));
    }
}
