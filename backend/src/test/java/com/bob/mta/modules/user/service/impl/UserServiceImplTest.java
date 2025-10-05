package com.bob.mta.modules.user.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.service.command.CreateUserCommand;
import com.bob.mta.modules.user.service.model.ActivationLink;
import com.bob.mta.modules.user.service.model.CreateUserResult;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;
import com.bob.mta.modules.user.service.query.UserQuery;
import com.bob.mta.modules.user.support.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceImplTest {

    private MutableClock clock;
    private FakeUserRepository repository;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        repository = new FakeUserRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        service = new UserServiceImpl(repository, passwordEncoder, clock, Duration.ofHours(24));
        seedActiveUser("admin", "系统管理员", "admin@example.com", "admin123", List.of("ROLE_ADMIN", "ROLE_OPERATOR"));
    }

    @Test
    void shouldCreateUserAndIssueActivationToken() {
        CreateUserCommand command = new CreateUserCommand(
                "new.user",
                "New User",
                "new.user@example.com",
                "password123",
                List.of("operator"));

        CreateUserResult result = service.createUser(command);

        assertThat(result.user().status()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(result.activation().token()).isNotBlank();
        assertThat(result.activation().expiresAt()).isEqualTo(Instant.parse("2024-01-02T00:00:00Z"));
        assertThat(passwordEncoder.matches("password123",
                repository.findById(result.user().id()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void shouldPreventDuplicateUsernames() {
        service.createUser(new CreateUserCommand("duplicate", "First", "duplicate1@example.com", "password123", List.of("operator")));

        CreateUserCommand duplicate = new CreateUserCommand(
                "duplicate",
                "Second",
                "duplicate2@example.com",
                "password123",
                List.of("operator"));

        assertThatThrownBy(() -> service.createUser(duplicate))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USERNAME_EXISTS);
    }

    @Test
    void shouldActivateUserWithValidToken() {
        CreateUserResult result = service.createUser(new CreateUserCommand(
                "activate.me",
                "Pending User",
                "activate@example.com",
                "password123",
                List.of("operator")));

        UserView activated = service.activateUser(result.activation().token());

        assertThat(activated.status()).isEqualTo(UserStatus.ACTIVE);
        assertThatThrownBy(() -> service.activateUser(result.activation().token()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_ACTIVE);
    }

    @Test
    void shouldRejectExpiredActivationToken() {
        CreateUserResult result = service.createUser(new CreateUserCommand(
                "expire.me",
                "Pending User",
                "expire@example.com",
                "password123",
                List.of("operator")));

        clock.advance(Duration.ofHours(25));

        assertThatThrownBy(() -> service.activateUser(result.activation().token()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACTIVATION_TOKEN_EXPIRED);
    }

    @Test
    void shouldResendActivationWithNewToken() {
        CreateUserResult result = service.createUser(new CreateUserCommand(
                "resend.me",
                "Pending User",
                "resend@example.com",
                "password123",
                List.of("operator")));

        ActivationLink resend = service.resendActivation(result.user().id());

        assertThat(resend.token()).isNotEqualTo(result.activation().token());
        assertThat(resend.expiresAt()).isEqualTo(Instant.parse("2024-01-02T00:00:00Z"));
    }

    @Test
    void shouldListUsersByStatus() {
        service.createUser(new CreateUserCommand(
                "pending.user",
                "Pending",
                "pending@example.com",
                "password123",
                List.of("operator")));

        List<UserView> activeUsers = service.listUsers(new UserQuery(UserStatus.ACTIVE));
        List<UserView> pendingUsers = service.listUsers(new UserQuery(UserStatus.PENDING_ACTIVATION));

        assertThat(activeUsers).extracting(UserView::status).containsOnly(UserStatus.ACTIVE);
        assertThat(pendingUsers).extracting(UserView::status).contains(UserStatus.PENDING_ACTIVATION);
    }

    @Test
    void shouldAuthenticateActiveUser() {
        UserAuthentication auth = service.authenticate("admin", "admin123");

        assertThat(auth.username()).isEqualTo("admin");
        assertThat(auth.roles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_OPERATOR");
    }

    @Test
    void shouldRejectInactiveUserDuringAuthentication() {
        CreateUserResult result = service.createUser(new CreateUserCommand(
                "inactive",
                "Inactive",
                "inactive@example.com",
                "password123",
                List.of("operator")));

        assertThatThrownBy(() -> service.authenticate("inactive", "password123"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_INACTIVE);

        service.activateUser(result.activation().token());
        UserAuthentication auth = service.authenticate("inactive", "password123");
        assertThat(auth.status()).isEqualTo(UserStatus.ACTIVE);
    }

    private void seedActiveUser(String username, String displayName, String email, String password, List<String> roles) {
        String id = UUID.randomUUID().toString();
        User user = new User(id, username, displayName, email, passwordEncoder.encode(password),
                UserStatus.ACTIVE, roles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roles));
        repository.seed(user);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }

}
