package com.github.igor.ecommerce.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String EMAIL = "igor@example.com";

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAuthenticateWhenAuthorizationHeaderIsMissing() throws Exception {
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(),
                recordingChain(chainInvoked));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chainInvoked).isTrue();
        verify(jwtService, never()).isValidToken(any());
    }

    @Test
    void shouldNotAuthenticateWhenAuthorizationSchemeIsNotBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, new MockHttpServletResponse(), recordingChain(new AtomicBoolean()));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).isValidToken(any());
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsInvalid() throws Exception {
        when(jwtService.isValidToken("invalid-token")).thenReturn(false);

        filter.doFilterInternal(requestWithBearer("invalid-token"), new MockHttpServletResponse(),
                recordingChain(new AtomicBoolean()));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void shouldAuthenticateWhenTokenIsValid() throws Exception {
        UserDetails userDetails = userDetailsWithRole("USER");
        when(jwtService.isValidToken("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);

        filter.doFilterInternal(requestWithBearer("valid-token"), new MockHttpServletResponse(),
                recordingChain(new AtomicBoolean()));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull()
                .satisfies(authentication -> {
                    assertThat(authentication.getName()).isEqualTo(EMAIL);
                    assertThat(authentication.getAuthorities())
                            .extracting(GrantedAuthority::getAuthority)
                            .containsExactly("ROLE_USER");
                });
    }

    @Test
    void shouldNotAuthenticateWhenUserNoLongerExists() throws Exception {
        when(jwtService.isValidToken("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilterInternal(requestWithBearer("valid-token"), new MockHttpServletResponse(),
                recordingChain(new AtomicBoolean()));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private UserDetails userDetailsWithRole(String role) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(EMAIL)
                .password("encoded-password")
                .roles(role)
                .build();
    }

    private FilterChain recordingChain(AtomicBoolean invoked) {
        return (request, response) -> invoked.set(true);
    }
}
