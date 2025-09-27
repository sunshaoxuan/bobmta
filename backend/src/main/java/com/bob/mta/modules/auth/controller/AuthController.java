package com.bob.mta.modules.auth.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginRequest;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints covering authentication lifecycle.
 */
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {
        return ApiResponse.success(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser(final Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "auth.required");
        }
        return ApiResponse.success(authService.currentUser(principal.getName()));
    }
}
