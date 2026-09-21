package com.github.igor.ecommerce.service.impl;

import com.github.igor.ecommerce.dto.request.LoginRequest;
import com.github.igor.ecommerce.dto.request.RegisterRequest;
import com.github.igor.ecommerce.dto.response.LoginResponse;
import com.github.igor.ecommerce.dto.response.UserResponse;
import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.enums.Role;
import com.github.igor.ecommerce.exception.ConflictException;
import com.github.igor.ecommerce.exception.UnauthorizedException;
import com.github.igor.ecommerce.repository.UserRepository;
import com.github.igor.ecommerce.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerShouldEncodePasswordAndCreateUserWithUserRole() {
        RegisterRequest request = new RegisterRequest("Igor", "igor@example.com", "plainPassword123");
        when(userRepository.existsByEmail("igor@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        UserResponse response = authService.register(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Igor");
        assertThat(response.email()).isEqualTo("igor@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("plainPassword123");
    }

    @Test
    void registerShouldThrowConflictExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("igor@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Igor", "igor@example.com", "plainPassword123")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("igor@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginShouldReturnTokenAndUserWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("igor@example.com", "plainPassword123");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        User user = new User("Igor", "igor@example.com", "hashed-password", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmail("igor@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.expiresIn()).isEqualTo(7200L);
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("igor@example.com");
        assertThat(response.user().role()).isEqualTo(Role.USER);
    }

    @Test
    void loginShouldThrowUnauthorizedExceptionWhenPasswordIsWrong() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("igor@example.com", "wrongPassword")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void loginShouldThrowUnauthorizedExceptionWhenEmailDoesNotExist() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "senhaForte123")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(jwtService, never()).generateToken(any(User.class));
    }
}
