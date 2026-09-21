package com.github.igor.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.security.JwtService;
import com.github.igor.ecommerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends IntegrationTestSupport {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

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
    void putCategoryWithUserTokenShouldReturn403() throws Exception {
        mockMvc.perform(bearer(put("/api/v1/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nova Categoria\"}"), userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/v1/categories/1"));
    }

    @Test
    void deleteCategoryWithUserTokenShouldReturn403() throws Exception {
        mockMvc.perform(bearer(delete("/api/v1/categories/{id}", 1L), userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/v1/categories/1"));
    }

    @Test
    void postProductWithUserTokenShouldReturn403() throws Exception {
        mockMvc.perform(bearer(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Produto\",\"price\":10.0,\"stockQuantity\":1,\"categoryId\":1}"), userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/v1/products"));
    }

    @Test
    void putProductWithUserTokenShouldReturn403() throws Exception {
        mockMvc.perform(bearer(put("/api/v1/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Produto alterado\"}"), userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/v1/products/1"));
    }

    @Test
    void deleteProductWithUserTokenShouldReturn403() throws Exception {
        mockMvc.perform(bearer(delete("/api/v1/products/{id}", 1L), userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/v1/products/1"));
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
    void getProductsWithTamperedSignatureShouldReturn401() throws Exception {
        String tampered = tamperSignature(adminToken);

        assertThat(jwtService.isValidToken(tampered)).isFalse();

        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Authentication is required")))
                .andExpect(jsonPath("$.path").value("/api/v1/products"));
    }

    @Test
    void getProductsWithExpiredTokenShouldReturn401() throws Exception {
        User admin = userRepository.findByEmail("admin@ecommerce.com").orElseThrow();
        String expiredToken = new JwtService(jwtSecret, -1).generateToken(admin);

        assertThat(jwtService.isValidToken(expiredToken)).isFalse();

        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
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

    private String tamperSignature(String token) {
        String[] parts = token.split("\\.");
        char firstChar = parts[2].charAt(0);
        char tamperedChar = firstChar == 'a' ? 'b' : 'a';
        return parts[0] + "." + parts[1] + "." + tamperedChar + parts[2].substring(1);
    }
}
