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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOrderControllerIntegrationTest extends IntegrationTestSupport {

    private static final String BASE_PATH = "/api/v1/admin/orders";
    private static final String CART_ITEMS_PATH = "/api/v1/cart/items";
    private static final String PRODUCTS_PATH = "/api/v1/products";
    private static final String ORDERS_PATH = "/api/v1/orders";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    private String otherUserToken;

    @BeforeEach
    void setUpOtherUser() {
        User otherUser = userRepository.save(new User(
                "Other Admin Order User",
                "other-admin-order-user@ecommerce.com",
                passwordEncoder.encode("password123"),
                Role.USER
        ));
        otherUserToken = jwtService.generateToken(otherUser);
    }

    @Test
    void listAllOrdersWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Authentication is required")))
                .andExpect(jsonPath("$.path").value(BASE_PATH));
    }

    @Test
    void listAllOrdersWithUserTokenShouldReturn403() throws Exception {
        mockMvc.perform(bearer(get(BASE_PATH), userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")))
                .andExpect(jsonPath("$.path").value(BASE_PATH));
    }

    @Test
    void listAllOrdersWithAdminTokenShouldReturn200WithOrdersFromAllUsers() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        long userOrderId = checkoutOrderId(userToken, productId, 1);
        long otherOrderId = checkoutOrderId(otherUserToken, productId, 1);

        mockMvc.perform(bearer(get(BASE_PATH), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(userOrderId))
                .andExpect(jsonPath("$.content[1].id").value(otherOrderId));
    }

    @Test
    void updateStatusShouldReturn200AdvancingThroughValidTransitions() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        long orderId = checkoutOrderId(userToken, productId, 2);

        patchStatus(orderId, "PAID")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PAID"));

        patchStatus(orderId, "SHIPPED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        patchStatus(orderId, "DELIVERED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void updateStatusShouldReturn400WhenTransitionIsInvalid() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        long orderId = checkoutOrderId(userToken, productId, 2);

        patchStatus(orderId, "DELIVERED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid status transition from PENDING to DELIVERED")))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/" + orderId + "/status"));
    }

    @Test
    void updateStatusShouldReturn400WhenStatusValueDoesNotExist() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        long orderId = checkoutOrderId(userToken, productId, 2);

        patchStatus(orderId, "FOO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Malformed JSON request")))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/" + orderId + "/status"));
    }

    @Test
    void updateStatusShouldReturn404WhenOrderDoesNotExist() throws Exception {
        expectErrorJson(patchStatus(999_999L, "PAID"), 404, BASE_PATH + "/999999/status");
    }

    @Test
    void cancelShouldReturn200AndRestoreStockOnlyOnce() throws Exception {
        Long productId = createProductAsAdmin("Curso de Java", "99.90", 10);
        long orderId = checkoutOrderId(userToken, productId, 4);

        mockMvc.perform(bearer(get(PRODUCTS_PATH + "/{id}", productId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(6));

        patchStatus(orderId, "PAID")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        patchStatus(orderId, "CANCELLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(bearer(get(PRODUCTS_PATH + "/{id}", productId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(10));

        patchStatus(orderId, "CANCELLED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Invalid status transition")))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/" + orderId + "/status"));

        mockMvc.perform(bearer(get(PRODUCTS_PATH + "/{id}", productId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(10));
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

    private long checkoutOrderId(String token, Long productId, int quantity) throws Exception {
        mockMvc.perform(bearer(post(CART_ITEMS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"), token))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(bearer(post(ORDERS_PATH), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions patchStatus(long orderId, String status) throws Exception {
        return mockMvc.perform(bearer(patch(BASE_PATH + "/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"), adminToken));
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
