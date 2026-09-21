package com.github.igor.ecommerce.security;

import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.enums.Role;
import com.github.igor.ecommerce.exception.NotFoundException;
import com.github.igor.ecommerce.exception.UnauthorizedException;
import com.github.igor.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserShouldReturnUserWhenAuthenticated() {
        User user = new User("Igor", "igor@example.com", "hashed-password", Role.USER);
        when(userRepository.findByEmail("igor@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(authentication("igor@example.com"));

        User result = currentUserService.getCurrentUser();

        assertThat(result).isSameAs(user);
    }

    @Test
    void getCurrentUserShouldThrowUnauthorizedExceptionWhenContextIsEmpty() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not authenticated");
    }

    @Test
    void getCurrentUserShouldThrowUnauthorizedExceptionWhenPrincipalIsNotUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("igor@example.com", null));

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUserShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(authentication("ghost@example.com"));

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ghost@example.com");
    }

    private Authentication authentication(String email) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password("hashed-password")
                .roles("USER")
                .build();
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
