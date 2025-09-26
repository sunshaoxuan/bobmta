package com.bob.mta.modules.user.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.user.dto.ActivateUserRequest;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.AssignRolesRequest;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.dto.UserResponse;
import com.bob.mta.modules.user.service.impl.InMemoryUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTest {

    private UserController controller;
    private InMemoryUserService userService;

    @BeforeEach
    void setUp() {
        userService = new InMemoryUserService(new BCryptPasswordEncoder());
        controller = new UserController(userService);
    }

    @Test
    void createUserShouldReturnResponse() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("controller");
        request.setDisplayName("Controller User");
        request.setEmail("controller@demo.com");

        ApiResponse<UserResponse> response = controller.createUser(request);

        assertThat(response.getData().getUsername()).isEqualTo("controller");
    }

    @Test
    void activationEndpointsShouldReturnTokens() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("to-activate");
        request.setDisplayName("To Activate");
        request.setEmail("activate@demo.com");
        String userId = controller.createUser(request).getData().getId();

        ActivationLinkResponse resend = controller.resendActivation(userId).getData();
        ActivateUserRequest activateUserRequest = new ActivateUserRequest();
        activateUserRequest.setToken(resend.getToken());
        ActivationLinkResponse activated = controller.activate(activateUserRequest).getData();

        assertThat(activated.getToken()).isEqualTo(resend.getToken());
    }

    @Test
    void assignRolesShouldUpdateUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("role-change");
        request.setDisplayName("Role Change");
        request.setEmail("role@demo.com");
        String userId = controller.createUser(request).getData().getId();

        AssignRolesRequest assign = new AssignRolesRequest();
        assign.setRoles(List.of("auditor"));

        UserResponse response = controller.assignRoles(userId, assign).getData();

        assertThat(response.getRoles()).containsExactly("ROLE_AUDITOR");
    }
}
