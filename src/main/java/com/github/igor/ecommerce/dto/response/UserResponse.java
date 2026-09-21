package com.github.igor.ecommerce.dto.response;

import com.github.igor.ecommerce.enums.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}
