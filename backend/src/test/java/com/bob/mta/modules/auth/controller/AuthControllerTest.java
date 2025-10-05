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
import java.security.Principal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {

    private AuthController controller;
    private InMemoryAuthService authService;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        InMemoryUserService userService = new InMemoryUserService(encoder);
        JwtTokenProvider provider = new JwtTokenProvider(jwtProperties());
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userService);
        authenticationProvider.setPasswordEncoder(encoder);
        AuthenticationManager authenticationManager = new ProviderManager(authenticationProvider);
        authService = new InMemoryAuthService(authenticationManager, provider, userService);
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
        Principal principal = () -> "admin";
        ApiResponse<CurrentUserResponse> response = controller.currentUser(principal);
        assertThat(response.getData().getUsername()).isEqualTo("admin");
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("unit-test");
        properties.getAccessToken().setSecret("test-secret-should-be-long-enough-123456789012345");
        return properties;
    }
}
