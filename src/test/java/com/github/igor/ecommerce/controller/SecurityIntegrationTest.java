package com.github.igor.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.igor.ecommerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends IntegrationTestSupport {

    @Test
    void getProductsWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Authentication is required")))
                .andExpect(jsonPath("$.path").value("/api/v1/products"));
    }

    @Test
    void getProductsWithUserTokenShouldReturn200() throws Exception {
        mockMvc.perform(bearer(get("/api/v1/products"), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void postCategoryWithUserTokenShouldReturn403() throws Exception {
        mockMvc.perform(bearer(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Segurança\"}"), userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")))
                .andExpect(jsonPath("$.path").value("/api/v1/categories"));
    }

    @Test
    void postCategoryWithAdminTokenShouldReturn201() throws Exception {
        mockMvc.perform(bearer(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Segurança\"}"), adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Segurança"));
    }

    @Test
    void getProductsWithInvalidTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/products"));
    }

    @Test
    void patchStockWithUserTokenShouldReturn403() throws Exception {
        mockMvc.perform(bearer(patch("/api/v1/products/{id}/stock", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginAsSeededAdminShouldReturnTokenThatAccessesAdminEndpoints() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@ecommerce.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = new ObjectMapper().readTree(body).get("token").asText();

        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cloud Computing\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void healthEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
