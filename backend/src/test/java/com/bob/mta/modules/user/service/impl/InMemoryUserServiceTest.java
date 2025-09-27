package com.bob.mta.modules.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.service.command.CreateUserCommand;
import com.bob.mta.modules.user.service.model.ActivationLink;
import com.bob.mta.modules.user.service.model.CreateUserResult;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;
import com.bob.mta.modules.user.service.query.UserQuery;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryUserServiceTest {

    private MutableClock clock;

    private InMemoryUserService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        service = new InMemoryUserService(clock);
        service.seedDefaultUsers();
    }

    @Test
    void shouldCreateUserAndIssueActivationToken() {
        final CreateUserCommand command = new CreateUserCommand(
                "new.user",
                "New User",
                "new.user@example.com",
                "password123",
                List.of("operator"));

        final CreateUserResult result = service.createUser(command);

        assertThat(result.user().status()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(result.activation().token()).isNotBlank();
        assertThat(result.activation().expiresAt()).isEqualTo(Instant.parse("2024-01-02T00:00:00Z"));
    }

    @Test
    void shouldPreventDuplicateUsernames() {
        final CreateUserCommand first = new CreateUserCommand(
                "duplicate",
                "First",
                "duplicate1@example.com",
                "password123",
                List.of("operator"));
        service.createUser(first);

        final CreateUserCommand duplicate = new CreateUserCommand(
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
        final CreateUserResult result = service.createUser(new CreateUserCommand(
                "activate.me",
                "Pending User",
                "activate@example.com",
                "password123",
                List.of("operator")));

        final UserView activated = service.activateUser(result.activation().token());

        assertThat(activated.status()).isEqualTo(UserStatus.ACTIVE);
        assertThatThrownBy(() -> service.activateUser(result.activation().token()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_ACTIVE);
    }

    @Test
    void shouldRejectExpiredActivationToken() {
        final CreateUserResult result = service.createUser(new CreateUserCommand(
                "expire.me",
                "Pending User",
                "expire@example.com",
                "password123",
                List.of("operator")));

        clock.advance(java.time.Duration.ofHours(25));

        assertThatThrownBy(() -> service.activateUser(result.activation().token()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACTIVATION_TOKEN_EXPIRED);
    }

    @Test
    void shouldResendActivationWithNewToken() {
        final CreateUserResult result = service.createUser(new CreateUserCommand(
                "resend.me",
                "Pending User",
                "resend@example.com",
                "password123",
                List.of("operator")));

        final ActivationLink resend = service.resendActivation(result.user().id());

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

        final List<UserView> activeUsers = service.listUsers(new UserQuery(UserStatus.ACTIVE));
        final List<UserView> pendingUsers = service.listUsers(new UserQuery(UserStatus.PENDING_ACTIVATION));

        assertThat(activeUsers).extracting(UserView::status).containsOnly(UserStatus.ACTIVE);
        assertThat(pendingUsers).extracting(UserView::status).containsOnly(UserStatus.PENDING_ACTIVATION);
    }

    @Test
    void shouldAuthenticateActiveUser() {
        final UserAuthentication auth = service.authenticate("admin", "admin123");

        assertThat(auth.username()).isEqualTo("admin");
        assertThat(auth.roles()).contains("ROLE_ADMIN");
    }

    @Test
    void shouldRejectInactiveUserDuringAuthentication() {
        final CreateUserResult result = service.createUser(new CreateUserCommand(
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
        final UserAuthentication auth = service.authenticate("inactive", "password123");
        assertThat(auth.status()).isEqualTo(UserStatus.ACTIVE);
    }

    private static final class MutableClock extends java.time.Clock {

        private Instant instant;

        private MutableClock(final Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public java.time.Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(final java.time.Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
