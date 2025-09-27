package com.bob.mta.modules.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.dto.ActivateUserRequest;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.AssignRolesRequest;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.dto.UserResponse;
import com.bob.mta.modules.user.service.impl.InMemoryUserService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserControllerTest {

    private UserController controller;
    private InMemoryUserService userService;

    @BeforeEach
    void setUp() {
        userService = new InMemoryUserService(new MutableClock(Instant.parse("2024-01-01T00:00:00Z")));
        userService.seedDefaultUsers();
        controller = new UserController(userService);
    }

    @Test
    void createUserShouldReturnResponse() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setUsername("controller");
        request.setDisplayName("Controller User");
        request.setEmail("controller@demo.com");
        request.setPassword("password123");

        final ApiResponse<UserResponse> response = controller.createUser(request);

        assertThat(response.getData().getUsername()).isEqualTo("controller");
        assertThat(response.getData().getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
    }

    @Test
    void activationEndpointsShouldActivateUser() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setUsername("to-activate");
        request.setDisplayName("To Activate");
        request.setEmail("activate@demo.com");
        request.setPassword("password123");
        final String userId = controller.createUser(request).getData().getId();

        final ActivationLinkResponse resend = controller.resendActivation(userId).getData();
        final ActivateUserRequest activateUserRequest = new ActivateUserRequest();
        activateUserRequest.setToken(resend.getToken());
        final ApiResponse<UserResponse> activated = controller.activate(activateUserRequest);

        assertThat(activated.getData().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void assignRolesShouldUpdateUser() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setUsername("role-change");
        request.setDisplayName("Role Change");
        request.setEmail("role@demo.com");
        request.setPassword("password123");
        final String userId = controller.createUser(request).getData().getId();

        final AssignRolesRequest assign = new AssignRolesRequest();
        assign.setRoles(List.of("auditor"));

        final UserResponse response = controller.assignRoles(userId, assign).getData();

        assertThat(response.getRoles()).containsExactly("ROLE_AUDITOR");
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
    }
}
