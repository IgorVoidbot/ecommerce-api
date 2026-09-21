package com.github.igor.ecommerce.controller;

import com.github.igor.ecommerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends IntegrationTestSupport {

    private static final String BASE_PATH = "/api/v1/auth";

    @Test
    void registerShouldReturn201WithUserSummary() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Igor Teste","email":"igor.test@example.com","password":"senhaForte123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Igor Teste"))
                .andExpect(jsonPath("$.email").value("igor.test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(userRepository.findByEmail("igor.test@example.com")).hasValueSatisfying(user -> {
            assertThat(user.getPassword()).isNotEqualTo("senhaForte123");
            assertThat(user.getPassword()).startsWith("$2");
            assertThat(user.getRole().name()).isEqualTo("USER");
        });
    }

    @Test
    void registerShouldReturn409WhenEmailAlreadyExists() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Admin","email":"admin@ecommerce.com","password":"senhaForte123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Email already in use")))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/register"));
    }

    @Test
    void registerShouldReturn400WithErrorsMapWhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Igor","email":"igor@example.com","password":"1234567"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 8 characters long"));
    }

    @Test
    void registerShouldReturn400WithErrorsMapWhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Igor","email":"not-an-email","password":"senhaForte123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Email must be a well-formed email address"));
    }

    @Test
    void loginShouldReturn200WithValidTokenForSeededAdmin() throws Exception {
        String body = mockMvc.perform(post(BASE_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@ecommerce.com","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(7200))
                .andExpect(jsonPath("$.user.email").value("admin@ecommerce.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("token").asText();
        assertThat(jwtService.isValidToken(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@ecommerce.com");
    }

    @Test
    void loginShouldReturn401WhenPasswordIsWrong() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@ecommerce.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.path").value(BASE_PATH + "/login"));
    }

    @Test
    void loginShouldReturn401WhenEmailDoesNotExist() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@example.com","password":"senhaForte123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
