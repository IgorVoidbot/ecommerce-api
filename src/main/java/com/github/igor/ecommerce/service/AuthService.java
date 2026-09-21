package com.github.igor.ecommerce.service;

import com.github.igor.ecommerce.dto.request.LoginRequest;
import com.github.igor.ecommerce.dto.request.RegisterRequest;
import com.github.igor.ecommerce.dto.response.LoginResponse;
import com.github.igor.ecommerce.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
