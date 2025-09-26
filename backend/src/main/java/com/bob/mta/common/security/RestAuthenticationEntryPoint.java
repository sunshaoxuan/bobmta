package com.bob.mta.common.security;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
<<<<<<< HEAD
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

=======
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Returns JSON response when anonymous users access protected resources.
 */
@Component
>>>>>>> origin/main
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

<<<<<<< HEAD
    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
=======
    public RestAuthenticationEntryPoint(final ObjectMapper objectMapper) {
>>>>>>> origin/main
        this.objectMapper = objectMapper;
    }

    @Override
<<<<<<< HEAD
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.AUTHENTICATION_FAILED;
        ApiResponse<Object> body = ApiResponse.failure(errorCode.getCode(), errorCode.getDefaultMessage());
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
=======
    public void commence(
            final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(ErrorCode.UNAUTHORIZED));
>>>>>>> origin/main
    }
}
