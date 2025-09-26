package com.bob.mta.modules.auth.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.security.JwtProperties;
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginRequest;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.impl.InMemoryAuthService;
import com.bob.mta.modules.user.service.impl.InMemoryUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {

    private AuthController controller;
    private InMemoryAuthService authService;

    @BeforeEach
    void setUp() {
        InMemoryUserService userService = new InMemoryUserService(new BCryptPasswordEncoder());
        JwtProperties properties = new JwtProperties();
        properties.getAccessToken().setSecret("a-very-long-secret-key-for-tests-1234567890");
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        authService = new InMemoryAuthService(userService, new BCryptPasswordEncoder(), provider);
        controller = new AuthController(authService);
    }

    @Test
    void loginShouldReturnToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        ApiResponse<LoginResponse> response = controller.login(request);

        assertThat(response.getData().getToken()).isNotBlank();
    }

    @Test
    void currentUserShouldReturnDetails() {
        UserDetails userDetails = authService.loadUserByUsername("admin");
        ApiResponse<CurrentUserResponse> response = controller.currentUser(userDetails);
        assertThat(response.getData().getUsername()).isEqualTo("admin");
    }
}
