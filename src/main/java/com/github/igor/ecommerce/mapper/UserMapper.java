package com.github.igor.ecommerce.mapper;

import com.github.igor.ecommerce.dto.response.UserResponse;
import com.github.igor.ecommerce.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
