package com.github.igor.ecommerce.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerIntegrationTest extends IntegrationTestSupport {

    private static final String BASE_PATH = "/api/v1/cart";
    private static final String ITEMS_PATH = BASE_PATH + "/items";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    private String otherUserToken;

    @BeforeEach
    void setUpOtherUser() {
        User otherUser = userRepository.save(new User(
                "Other User",
                "other-cart-user@ecommerce.com",
                passwordEncoder.encode("password123"),
                Role.USER
        ));
        otherUserToken = jwtService.generateToken(otherUser);
    }

    @Test
    void getCartWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Authentication is required")))
                .andExpect(jsonPath("$.path").value(BASE_PATH));
    }

    @Test
    void getCartShouldReturn200WithEmptyItemsAndZeroTotalWhenUserHasNoCart() throws Exception {
        mockMvc.perform(bearer(get(BASE_PATH), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void addItemShouldReturn201WithItemDetailsAndCorrectTotal() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);

        mockMvc.perform(bearer(post(ITEMS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":2}"), userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].productName").value("Curso de Java"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(99.90))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(199.80))
                .andExpect(jsonPath("$.totalAmount").value(199.80));
    }

    @Test
    void addItemShouldReturn404WhenProductDoesNotExist() throws Exception {
        expectErrorJson(mockMvc.perform(bearer(post(ITEMS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":999999,\"quantity\":1}"), userToken)),
                404, ITEMS_PATH);
    }

    @Test
    void addItemShouldReturn400WithErrorsMapWhenQuantityIsZeroOrNegative() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);

        assertValidationError(addItem(userToken, productId, 0));
        assertValidationError(addItem(userToken, productId, -1));
    }

    @Test
    void addItemTwiceShouldMergeQuantityIntoSingleItem() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);

        addItem(userToken, productId, 2).andExpect(status().isCreated());

        mockMvc.perform(bearer(post(ITEMS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":3}"), userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.items[0].subtotal").value(499.50))
                .andExpect(jsonPath("$.totalAmount").value(499.50));

        mockMvc.perform(bearer(get(BASE_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void addItemShouldReturn400WhenResultingQuantityExceedsStock() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 5);

        addItem(userToken, productId, 5).andExpect(status().isCreated());

        mockMvc.perform(bearer(post(ITEMS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"), userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("exceeds available stock")))
                .andExpect(jsonPath("$.path").value(ITEMS_PATH));

        mockMvc.perform(bearer(get(BASE_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void updateItemQuantityShouldReplaceQuantityAndRecalculateTotal() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        Long itemId = addItemAndGetItemId(userToken, productId, 2);

        mockMvc.perform(bearer(put(BASE_PATH + "/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":7}"), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(itemId))
                .andExpect(jsonPath("$.items[0].quantity").value(7))
                .andExpect(jsonPath("$.items[0].subtotal").value(699.30))
                .andExpect(jsonPath("$.totalAmount").value(699.30));
    }

    @Test
    void updateItemQuantityShouldReturn400WhenQuantityExceedsStock() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 5);
        Long itemId = addItemAndGetItemId(userToken, productId, 2);

        mockMvc.perform(bearer(put(BASE_PATH + "/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":6}"), userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("exceeds available stock")))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/items/" + itemId));

        mockMvc.perform(bearer(get(BASE_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void updateItemQuantityShouldReturn400WithErrorsMapWhenQuantityIsZeroOrNegative() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        Long itemId = addItemAndGetItemId(userToken, productId, 2);

        assertUpdateValidationError(itemId, 0);
        assertUpdateValidationError(itemId, -1);
    }

    @Test
    void updateItemShouldReturn404WhenItemBelongsToAnotherUser() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        Long otherItemId = addItemAndGetItemId(otherUserToken, productId, 2);

        mockMvc.perform(bearer(put(BASE_PATH + "/items/{itemId}", otherItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"), userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Cart item not found")))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/items/" + otherItemId));
    }

    @Test
    void deleteItemShouldReturn204AndRemoveItemFromCart() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        Long itemId = addItemAndGetItemId(userToken, productId, 2);

        mockMvc.perform(bearer(delete(BASE_PATH + "/items/{itemId}", itemId), userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(bearer(get(BASE_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void deleteItemShouldReturn404WhenItemBelongsToAnotherUser() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        Long otherItemId = addItemAndGetItemId(otherUserToken, productId, 2);

        mockMvc.perform(bearer(delete(BASE_PATH + "/items/{itemId}", otherItemId), userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Cart item not found")))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/items/" + otherItemId));
    }

    @Test
    void clearCartShouldReturn204AndEmptyTheCart() throws Exception {
        Long javaCourseId = createProductAsAdmin("Curso de Java", "99.90", 10);
        Long pythonCourseId = createProductAsAdmin("Curso de Python", "149.90", 10);
        addItem(userToken, javaCourseId, 2).andExpect(status().isCreated());
        addItem(userToken, pythonCourseId, 1).andExpect(status().isCreated());

        mockMvc.perform(bearer(delete(BASE_PATH), userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(bearer(get(BASE_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void getCartShouldNotExposeOtherUsersItems() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        addItem(otherUserToken, productId, 2).andExpect(status().isCreated());

        mockMvc.perform(bearer(get(BASE_PATH), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount").value(0));

        mockMvc.perform(bearer(get(BASE_PATH), otherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(199.80));
    }

    private Long createProductAsAdmin(String name, String price, int stock) throws Exception {
        Long categoryId = categoryRepository.findAll().stream()
                .filter(category -> category.getName().equals("Programação"))
                .map(Category::getId)
                .findFirst()
                .orElseThrow();
        String body = mockMvc.perform(bearer(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"price\":" + price
                                + ",\"stockQuantity\":" + stock + ",\"categoryId\":" + categoryId + "}"), adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions addItem(String token, Long productId, int quantity) throws Exception {
        return mockMvc.perform(bearer(post(ITEMS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"), token));
    }

    private Long addItemAndGetItemId(String token, Long productId, int quantity) throws Exception {
        String body = addItem(token, productId, quantity)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("items").get(0).get("id").asLong();
    }

    private void assertValidationError(ResultActions actions) throws Exception {
        actions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value(ITEMS_PATH))
                .andExpect(jsonPath("$.errors.quantity").value("Quantity must be at least 1"));
    }

    private void assertUpdateValidationError(Long itemId, int quantity) throws Exception {
        mockMvc.perform(bearer(put(BASE_PATH + "/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":" + quantity + "}"), userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/items/" + itemId))
                .andExpect(jsonPath("$.errors.quantity").value("Quantity must be at least 1"));
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
