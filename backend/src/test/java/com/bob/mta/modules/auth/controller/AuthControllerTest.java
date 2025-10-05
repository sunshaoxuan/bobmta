package com.bob.mta.modules.auth.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.security.JwtProperties;
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginRequest;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.impl.DefaultAuthService;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
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

class AuthControllerTest {

    private AuthController controller;
    private DefaultAuthService authService;
    private FakeUserRepository repository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        repository = new FakeUserRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        UserServiceImpl userService = new UserServiceImpl(repository, passwordEncoder, ClockFixed.fixed(), Duration.ofHours(24));
        seedUser("admin", "系统管理员", "admin@example.com", "admin123", List.of("ROLE_ADMIN"));
        seedUser("operator", "运维专员", "operator@example.com", "operator123", List.of("ROLE_OPERATOR"));
        JwtProperties properties = new JwtProperties();
        properties.getAccessToken().setSecret("a-very-long-secret-key-for-tests-1234567890");
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        authService = new DefaultAuthService(provider, properties, userService, repository);
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
        assertThat(response.getData().getRoles()).contains("ROLE_ADMIN");
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
