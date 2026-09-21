package com.github.igor.ecommerce.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.igor.ecommerce.entity.Category;
import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.enums.Role;
import com.github.igor.ecommerce.repository.CategoryRepository;
import com.github.igor.ecommerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIntegrationTest extends IntegrationTestSupport {

    private static final String BASE_PATH = "/api/v1/orders";
    private static final String CART_PATH = "/api/v1/cart";
    private static final String CART_ITEMS_PATH = CART_PATH + "/items";
    private static final String PRODUCTS_PATH = "/api/v1/products";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    private String otherUserToken;

    @BeforeEach
    void setUpOtherUser() {
        User otherUser = userRepository.save(new User(
                "Other Order User",
                "other-order-user@ecommerce.com",
                passwordEncoder.encode("password123"),
                Role.USER
        ));
        otherUserToken = jwtService.generateToken(otherUser);
    }

    @Test
    void checkoutWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(post(BASE_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Authentication is required")))
                .andExpect(jsonPath("$.path").value(BASE_PATH));
    }

    @Test
    void checkoutWithEmptyCartShouldReturn400() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        Long itemId = addItemAndGetItemId(userToken, productId, 1);

        mockMvc.perform(bearer(delete(CART_ITEMS_PATH + "/{itemId}", itemId), userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(bearer(post(BASE_PATH), userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("empty cart")))
                .andExpect(jsonPath("$.path").value(BASE_PATH));
    }

    @Test
    void checkoutShouldReturn201WithPendingOrderAndItemSnapshots() throws Exception {
        Long javaCourseId = createProductAsAdmin("Curso de Java", "99.90", 10);
        Long pythonCourseId = createProductAsAdmin("Curso de Python", "149.90", 5);
        addItem(userToken, javaCourseId, 2).andExpect(status().isCreated());
        addItem(userToken, pythonCourseId, 1).andExpect(status().isCreated());

        mockMvc.perform(bearer(post(BASE_PATH), userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(349.70))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].productId").value(javaCourseId))
                .andExpect(jsonPath("$.items[0].productName").value("Curso de Java"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(99.90))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(199.80))
                .andExpect(jsonPath("$.items[1].productId").value(pythonCourseId))
                .andExpect(jsonPath("$.items[1].productName").value("Curso de Python"))
                .andExpect(jsonPath("$.items[1].unitPrice").value(149.90))
                .andExpect(jsonPath("$.items[1].quantity").value(1))
                .andExpect(jsonPath("$.items[1].subtotal").value(149.90));
    }

    @Test
    void checkoutShouldDecrementStockAndClearCart() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        addItem(userToken, productId, 2).andExpect(status().isCreated());

        mockMvc.perform(bearer(post(BASE_PATH), userToken))
                .andExpect(status().isCreated());

        mockMvc.perform(bearer(get(PRODUCTS_PATH + "/{id}", productId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(8));

        mockMvc.perform(bearer(get(CART_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void checkoutShouldReturn400WithoutPartialEffectsWhenStockIsInsufficient() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        addItem(userToken, productId, 5).andExpect(status().isCreated());

        mockMvc.perform(bearer(patch(PRODUCTS_PATH + "/{id}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-7}"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(3));

        mockMvc.perform(bearer(post(BASE_PATH), userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("exceeds available stock")))
                .andExpect(jsonPath("$.path").value(BASE_PATH));

        mockMvc.perform(bearer(get(PRODUCTS_PATH + "/{id}", productId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(3));

        mockMvc.perform(bearer(get(CART_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalAmount").value(499.50));

        mockMvc.perform(bearer(get(BASE_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listMyOrdersShouldReturnPaginatedOrdersOfCurrentUserOnly() throws Exception {
        Long javaCourseId = createProductAsAdmin("Curso de Java", "99.90", 10);
        Long pythonCourseId = createProductAsAdmin("Curso de Python", "149.90", 10);
        JsonNode firstOrder = checkoutAndGetResponse(userToken, javaCourseId, 1);
        checkoutAndGetResponse(otherUserToken, javaCourseId, 1);
        JsonNode secondOrder = checkoutAndGetResponse(userToken, pythonCourseId, 2);

        mockMvc.perform(bearer(get(BASE_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(firstOrder.get("id").asLong()))
                .andExpect(jsonPath("$.content[1].id").value(secondOrder.get("id").asLong()))
                .andExpect(jsonPath("$.content[0].userId").value(firstOrder.get("userId").asLong()))
                .andExpect(jsonPath("$.content[1].userId").value(secondOrder.get("userId").asLong()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[1].status").value("PENDING"));
    }

    @Test
    void findMyOrderByIdShouldReturn200WhenOrderBelongsToCurrentUser() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        JsonNode order = checkoutAndGetResponse(userToken, productId, 2);
        long orderId = order.get("id").asLong();

        mockMvc.perform(bearer(get(BASE_PATH + "/{id}", orderId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value(order.get("userId").asLong()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(199.80))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].productName").value("Curso de Java"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(99.90))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(199.80));
    }

    @Test
    void findMyOrderByIdShouldReturn403WhenOrderBelongsToAnotherUser() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        long orderId = checkoutAndGetResponse(userToken, productId, 1).get("id").asLong();

        mockMvc.perform(bearer(get(BASE_PATH + "/{id}", orderId), otherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(containsString("not allowed")))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/" + orderId));
    }

    @Test
    void findMyOrderByIdShouldReturn404WhenOrderDoesNotExist() throws Exception {
        expectErrorJson(mockMvc.perform(bearer(get(BASE_PATH + "/{id}", 999_999L), userToken)),
                404, BASE_PATH + "/999999");
    }

    private Long createProductAsAdmin(String name, String price, int stock) throws Exception {
        Long categoryId = categoryRepository.findAll().stream()
                .filter(category -> category.getName().equals("Programação"))
                .map(Category::getId)
                .findFirst()
                .orElseThrow();
        String body = mockMvc.perform(bearer(post(PRODUCTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"price\":" + price
                                + ",\"stockQuantity\":" + stock + ",\"categoryId\":" + categoryId + "}"), adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions addItem(String token, Long productId, int quantity) throws Exception {
        return mockMvc.perform(bearer(post(CART_ITEMS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"), token));
    }

    private Long addItemAndGetItemId(String token, Long productId, int quantity) throws Exception {
        String body = addItem(token, productId, quantity)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("items").get(0).get("id").asLong();
    }

    private JsonNode checkoutAndGetResponse(String token, Long productId, int quantity) throws Exception {
        addItem(token, productId, quantity).andExpect(status().isCreated());
        String body = mockMvc.perform(bearer(post(BASE_PATH), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private void expectErrorJson(ResultActions actions, int expectedStatus, String expectedPath) throws Exception {
        actions.andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(expectedPath));
    }
}
