package com.bob.mta.common.security;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
<<<<<<< HEAD
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

=======
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Ensures forbidden responses follow the platform API contract.
 */
@Component
>>>>>>> origin/main
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

<<<<<<< HEAD
    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
=======
    public RestAccessDeniedHandler(final ObjectMapper objectMapper) {
>>>>>>> origin/main
        this.objectMapper = objectMapper;
    }

    @Override
<<<<<<< HEAD
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        ApiResponse<Object> body = ApiResponse.failure(errorCode.getCode(), errorCode.getDefaultMessage());
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
=======
    public void handle(
            final HttpServletRequest request, final HttpServletResponse response, final AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(ErrorCode.FORBIDDEN));
>>>>>>> origin/main
    }
}
