package com.github.igor.ecommerce.dto.response;

public record LoginResponse(
        String token,
        long expiresIn,
        String type,
        UserResponse user
) {
}
