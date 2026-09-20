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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerIntegrationTest {

    private static final String BASE_PATH = "/api/v1/products";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createShouldReturn201WhenCategoryExists() throws Exception {
        Long categoryId = categoryIdByName("Programação");

        performPost("""
                {"name":"Curso de Java","description":"Do zero ao avançado","price":99.90,"stockQuantity":10,"categoryId":%d}
                """.formatted(categoryId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Curso de Java"))
                .andExpect(jsonPath("$.description").value("Do zero ao avançado"))
                .andExpect(jsonPath("$.price").value(99.90))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Programação"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void createShouldReturn404WhenCategoryDoesNotExist() throws Exception {
        expectErrorJson(performPost("""
                {"name":"Curso de Java","price":99.90,"stockQuantity":10,"categoryId":999999}
                """),
                404, BASE_PATH);
    }

    @Test
    void createShouldReturn400WhenPriceIsNotPositive() throws Exception {
        Long categoryId = categoryIdByName("Programação");

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Curso\",\"price\":0,\"stockQuantity\":5,\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value(BASE_PATH))
                .andExpect(jsonPath("$.errors.price").value("Price must be greater than zero"));
    }

    @Test
    void createShouldReturn400WhenStockQuantityIsNegative() throws Exception {
        Long categoryId = categoryIdByName("Programação");

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Curso\",\"price\":99.90,\"stockQuantity\":-1,\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.stockQuantity").value("Stock quantity must be zero or greater"));
    }

    @Test
    void createShouldReturn400WhenNameIsBlank() throws Exception {
        Long categoryId = categoryIdByName("Programação");

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"price\":99.90,\"stockQuantity\":5,\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
    }

    @Test
    void listAllShouldReturnPaginatedPageResponse() throws Exception {
        seedThreeProducts();

        mockMvc.perform(get(BASE_PATH)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Curso de Java"))
                .andExpect(jsonPath("$.content[1].name").value("Curso de Python"));
    }

    @Test
    void listAllShouldFilterByNameIgnoringCase() throws Exception {
        seedThreeProducts();

        mockMvc.perform(get(BASE_PATH).param("name", "CURSO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].name",
                        containsInAnyOrder("Curso de Java", "Curso de Python")));
    }

    @Test
    void listAllShouldFilterByCategoryId() throws Exception {
        Long programacaoId = categoryIdByName("Programação");
        Long mobileId = categoryIdByName("Mobile");
        saveProduct("Curso de Java", new BigDecimal("99.90"), 5, programacaoId);
        saveProduct("Ebook Docker", new BigDecimal("59.90"), 3, mobileId);

        mockMvc.perform(get(BASE_PATH).param("categoryId", mobileId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Ebook Docker"))
                .andExpect(jsonPath("$.content[0].categoryId").value(mobileId));
    }

    @Test
    void findByIdShouldReturn200WhenProductExists() throws Exception {
        Long categoryId = categoryIdByName("Programação");
        Product product = saveProduct("Curso de Java", new BigDecimal("99.90"), 10, categoryId);

        mockMvc.perform(get(BASE_PATH + "/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Curso de Java"))
                .andExpect(jsonPath("$.price").value(99.90))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.categoryName").value("Programação"));
    }

    @Test
    void findByIdShouldReturn404WhenProductDoesNotExist() throws Exception {
        expectErrorJson(mockMvc.perform(get(BASE_PATH + "/{id}", 999_999L)),
                404, BASE_PATH + "/999999");
    }

    @Test
    void updateShouldReturn200ApplyingOnlyProvidedFields() throws Exception {
        Long programacaoId = categoryIdByName("Programação");
        Long mobileId = categoryIdByName("Mobile");
        Product product = saveProduct("Curso de Java", new BigDecimal("99.90"), 10, programacaoId);

        mockMvc.perform(put(BASE_PATH + "/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Curso de Java 21\",\"price\":119.90,\"categoryId\":" + mobileId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Curso de Java 21"))
                .andExpect(jsonPath("$.price").value(119.90))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.categoryId").value(mobileId))
                .andExpect(jsonPath("$.categoryName").value("Mobile"));

        Product persisted = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Curso de Java 21");
        assertThat(persisted.getPrice()).isEqualByComparingTo("119.90");
        assertThat(persisted.getCategory().getId()).isEqualTo(mobileId);
    }

    @Test
    void updateShouldReturn404WhenProductDoesNotExist() throws Exception {
        mockMvc.perform(put(BASE_PATH + "/{id}", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Novo nome\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/999999"));
    }

    @Test
    void deleteShouldReturn204AndRemoveProduct() throws Exception {
        Long categoryId = categoryIdByName("Programação");
        Product product = saveProduct("Curso de Java", new BigDecimal("99.90"), 10, categoryId);

        mockMvc.perform(delete(BASE_PATH + "/{id}", product.getId()))
                .andExpect(status().isNoContent());

        assertThat(productRepository.findById(product.getId())).isEmpty();
    }

    @Test
    void deleteShouldReturn404WhenProductDoesNotExist() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/999999"));
    }

    @Test
    void adjustStockShouldReturn200IncreasingQuantity() throws Exception {
        Long categoryId = categoryIdByName("Programação");
        Product product = saveProduct("Curso de Java", new BigDecimal("99.90"), 5, categoryId);

        performStockPatch(product.getId(), 10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.stockQuantity").value(15));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(15);
    }

    @Test
    void adjustStockShouldReturn200DecreasingQuantity() throws Exception {
        Long categoryId = categoryIdByName("Programação");
        Product product = saveProduct("Curso de Java", new BigDecimal("99.90"), 20, categoryId);

        performStockPatch(product.getId(), -10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @Test
    void adjustStockShouldReturn400WhenStockWouldBecomeNegative() throws Exception {
        Long categoryId = categoryIdByName("Programação");
        Product product = saveProduct("Curso de Java", new BigDecimal("99.90"), 5, categoryId);

        mockMvc.perform(patch(BASE_PATH + "/{id}/stock", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Insufficient stock")))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/" + product.getId() + "/stock"));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
    }

    @Test
    void adjustStockShouldReturn400WhenQuantityIsZero() throws Exception {
        Long categoryId = categoryIdByName("Programação");
        Product product = saveProduct("Curso de Java", new BigDecimal("99.90"), 5, categoryId);

        performStockPatch(product.getId(), 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Quantity must not be zero")));
    }

    @Test
    void adjustStockShouldReturn400WhenQuantityIsNull() throws Exception {
        Long categoryId = categoryIdByName("Programação");
        Product product = saveProduct("Curso de Java", new BigDecimal("99.90"), 5, categoryId);

        mockMvc.perform(patch(BASE_PATH + "/{id}/stock", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").value("Quantity is required"));
    }

    @Test
    void adjustStockShouldReturn404WhenProductDoesNotExist() throws Exception {
        expectErrorJson(mockMvc.perform(patch(BASE_PATH + "/{id}/stock", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}")),
                404, BASE_PATH + "/999999/stock");
    }

    private ResultActions performPost(String body) throws Exception {
        return mockMvc.perform(post(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions performStockPatch(Long id, int quantity) throws Exception {
        return mockMvc.perform(patch(BASE_PATH + "/{id}/stock", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":" + quantity + "}"));
    }

    private void expectErrorJson(ResultActions actions, int expectedStatus, String expectedPath) throws Exception {
        actions.andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(expectedPath));
    }

    private void seedThreeProducts() {
        Long programacaoId = categoryIdByName("Programação");
        Long mobileId = categoryIdByName("Mobile");
        saveProduct("Curso de Java", new BigDecimal("99.90"), 5, programacaoId);
        saveProduct("Curso de Python", new BigDecimal("149.90"), 8, programacaoId);
        saveProduct("Ebook Docker", new BigDecimal("59.90"), 3, mobileId);
    }

    private Product saveProduct(String name, BigDecimal price, int stock, Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        return productRepository.save(new Product(name, "descrição padrão", price, stock, category));
    }

    private Long categoryIdByName(String name) {
        return categoryRepository.findAll().stream()
                .filter(category -> category.getName().equals(name))
                .map(Category::getId)
                .findFirst()
                .orElseThrow();
    }
}
