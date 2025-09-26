package com.bob.mta.modules.user.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryUserServiceTest {

    private InMemoryUserService userService;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        userService = new InMemoryUserService(encoder);
    }

    @Test
    void createUserShouldPersistPendingAccount() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setDisplayName("New User");
        request.setEmail("new@demo.com");
        request.setRoles(List.of("admin"));

        User user = userService.createUser(request);

        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(userService.findByUsername("newuser")).isPresent();
    }

    @Test
    void createUserShouldFailWhenUsernameExists() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("admin");
        request.setDisplayName("Dup");
        request.setEmail("dup@demo.com");

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USERNAME_EXISTS.getCode());
    }

    @Test
    void activationFlowShouldUpdateStatus() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("activate");
        request.setDisplayName("Need Activation");
        request.setEmail("activate@demo.com");
        User user = userService.createUser(request);

        ActivationLinkResponse resend = userService.resendActivation(user.getId());
        ActivationLinkResponse activation = userService.activateUser(resend.getToken());

        assertThat(activation.getToken()).isEqualTo(resend.getToken());
        assertThat(userService.getById(user.getId()).getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void assignRolesShouldNormalizeRoleNames() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("roleuser");
        request.setDisplayName("Role User");
        request.setEmail("role@demo.com");
        User user = userService.createUser(request);

        User updated = userService.assignRoles(user.getId(), List.of("viewer", "ROLE_operator"));

        assertThat(updated.getRoles()).containsExactlyInAnyOrder("ROLE_VIEWER", "ROLE_OPERATOR");
    }
}
