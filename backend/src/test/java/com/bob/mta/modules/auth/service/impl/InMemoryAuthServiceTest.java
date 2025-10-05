package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.security.JwtProperties;
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.service.impl.InMemoryUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAuthServiceTest {

    private InMemoryAuthService authService;
    private InMemoryUserService userService;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        userService = new InMemoryUserService(encoder);
        JwtProperties properties = jwtProperties();
        tokenProvider = new JwtTokenProvider(properties);
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userService);
        authenticationProvider.setPasswordEncoder(encoder);
        AuthenticationManager authenticationManager = new ProviderManager(authenticationProvider);
        authService = new InMemoryAuthService(authenticationManager, tokenProvider, userService);
    }

    @Test
    void loginShouldReturnTokenForValidUser() {
        LoginResponse response = authService.login("admin", "admin123");

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getRoles()).contains("ROLE_ADMIN");
        assertThat(tokenProvider.parseToken(response.getToken())).isPresent();
    }

    @Test
    void loginShouldFailForInactiveUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("inactive");
        request.setDisplayName("Inactive");
        request.setEmail("inactive@demo.com");
        request.setPassword("password123");
        userService.createUser(request);

        assertThatThrownBy(() -> authService.login("inactive", "whatever"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_INACTIVE.getCode());
    }

    @Test
    void currentUserShouldMapAuthorities() {
        CurrentUserResponse current = authService.currentUser("admin");
        assertThat(current.getUsername()).isEqualTo("admin");
        assertThat(current.getRoles()).contains("ROLE_ADMIN");
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("unit-test");
        properties.getAccessToken().setSecret("unit-test-secret-must-be-longer-than-32-bytes-1234");
        return properties;
    }
}
