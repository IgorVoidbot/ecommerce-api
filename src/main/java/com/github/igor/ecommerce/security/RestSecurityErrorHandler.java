package com.github.igor.ecommerce.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String MESSAGE_AUTHENTICATION = "Authentication is required to access this resource";
    private static final String MESSAGE_ACCESS_DENIED = "Access denied: you do not have sufficient permissions";

    private final ObjectMapper objectMapper;

    public RestSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeProblemDetail(request, response, HttpStatus.UNAUTHORIZED, MESSAGE_AUTHENTICATION);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        writeProblemDetail(request, response, HttpStatus.FORBIDDEN, MESSAGE_ACCESS_DENIED);
    }

    private void writeProblemDetail(HttpServletRequest request, HttpServletResponse response,
                                    HttpStatus status, String message) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setProperty("timestamp", OffsetDateTime.now().toString());
        problemDetail.setProperty("error", status.getReasonPhrase());
        problemDetail.setProperty("message", message);
        problemDetail.setProperty("path", request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
