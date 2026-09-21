package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.request.LoginRequest;
import com.github.igor.ecommerce.dto.request.RegisterRequest;
import com.github.igor.ecommerce.dto.response.LoginResponse;
import com.github.igor.ecommerce.dto.response.UserResponse;
import com.github.igor.ecommerce.entity.Cart;
import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.enums.Role;
import com.github.igor.ecommerce.exception.ConflictException;
import com.github.igor.ecommerce.exception.UnauthorizedException;
import com.github.igor.ecommerce.mapper.UserMapper;
import com.github.igor.ecommerce.repository.CartRepository;
import com.github.igor.ecommerce.repository.UserRepository;
import com.github.igor.ecommerce.security.JwtService;
import com.github.igor.ecommerce.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           CartRepository cartRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }
        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );
        User savedUser = userRepository.save(user);
        cartRepository.save(new Cart(savedUser));
        return UserMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return new LoginResponse(
                jwtService.generateToken(user),
                jwtService.getExpirationSeconds(),
                "Bearer",
                UserMapper.toResponse(user)
        );
    }
}
