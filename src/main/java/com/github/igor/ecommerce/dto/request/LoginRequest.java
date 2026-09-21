package com.github.igor.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a well-formed email address")
        String email,
        @NotBlank(message = "Password is required")
        String password
) {
}
