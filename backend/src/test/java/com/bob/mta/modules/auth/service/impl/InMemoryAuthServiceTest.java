package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.security.JwtProperties;
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginRequest;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.service.impl.InMemoryUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAuthServiceTest {

    private InMemoryAuthService authService;
    private InMemoryUserService userService;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        userService = new InMemoryUserService(encoder);
        JwtProperties properties = new JwtProperties();
        properties.getAccessToken().setSecret("a-very-long-secret-key-for-tests-1234567890");
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        authService = new InMemoryAuthService(userService, encoder, provider);
    }

    @Test
    void loginShouldReturnTokenForValidUser() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getRoles()).contains("ROLE_ADMIN");
    }

    @Test
    void loginShouldFailForInactiveUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("inactive");
        request.setDisplayName("Inactive");
        request.setEmail("inactive@demo.com");
        userService.createUser(request);

        LoginRequest login = new LoginRequest();
        login.setUsername("inactive");
        login.setPassword("whatever");

        assertThatThrownBy(() -> authService.login(login))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_INACTIVE.getCode());
    }

    @Test
    void loadUserByUsernameShouldReturnUserDetails() {
        UserDetails userDetails = authService.loadUserByUsername("admin");
        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
    }

    @Test
    void currentUserShouldMapAuthorities() {
        UserDetails userDetails = authService.loadUserByUsername("admin");
        CurrentUserResponse current = authService.currentUser(userDetails);
        assertThat(current.getUsername()).isEqualTo("admin");
        assertThat(current.getRoles()).contains("ROLE_ADMIN");
    }
}
