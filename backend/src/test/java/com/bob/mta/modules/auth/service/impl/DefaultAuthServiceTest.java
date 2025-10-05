package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.security.JwtProperties;
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginRequest;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.service.impl.UserServiceImpl;
import com.bob.mta.modules.user.support.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAuthServiceTest {

    private DefaultAuthService authService;
    private FakeUserRepository repository;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        repository = new FakeUserRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserServiceImpl(repository, passwordEncoder, ClockFixed.fixed(), Duration.ofHours(24));
        seedUser("admin", "系统管理员", "admin@example.com", "admin123", List.of("ROLE_ADMIN"));
        JwtProperties properties = new JwtProperties();
        properties.getAccessToken().setSecret("a-very-long-secret-key-for-tests-1234567890");
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        authService = new DefaultAuthService(provider, properties, userService, repository);
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
        request.setPassword("password123");
        userService.createUser(request);

        LoginRequest login = new LoginRequest();
        login.setUsername("inactive");
        login.setPassword("password123");

        assertThatThrownBy(() -> authService.login(login))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_INACTIVE);
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
        CurrentUserResponse current = authService.currentUser(userDetails.getUsername());
        assertThat(current.getUsername()).isEqualTo("admin");
        assertThat(current.getRoles()).contains("ROLE_ADMIN");
    }

    private void seedUser(String username, String displayName, String email, String password, List<String> roles) {
        User user = new User(UUID.randomUUID().toString(), username, displayName, email,
                passwordEncoder.encode(password), UserStatus.ACTIVE, new LinkedHashSet<>(roles));
        repository.seed(user);
    }

    private static final class ClockFixed extends java.time.Clock {

        private static ClockFixed fixed() {
            return new ClockFixed();
        }

        @Override
        public java.time.ZoneId getZone() {
            return java.time.ZoneOffset.UTC;
        }

        @Override
        public java.time.Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.parse("2024-01-01T00:00:00Z");
        }
    }
}
