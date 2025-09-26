package com.bob.mta.modules.auth.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginRequest;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success(authService.currentUser(userDetails));
    }
}
