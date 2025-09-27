package com.bob.mta.modules.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.audit.domain.AuditLog;
import com.bob.mta.modules.audit.service.AuditQuery;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.dto.ActivateUserRequest;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.AssignRolesRequest;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.dto.CreateUserResponse;
import com.bob.mta.modules.user.dto.UserResponse;
import com.bob.mta.modules.user.service.impl.InMemoryUserService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;

class UserControllerTest {

    private UserController controller;
    private InMemoryUserService userService;
    private InMemoryAuditService auditService;
    private MessageResolver messageResolver;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        userService = new InMemoryUserService(new MutableClock(Instant.parse("2024-01-01T00:00:00Z")));
        userService.seedDefaultUsers();
        auditService = new InMemoryAuditService();
        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        messageResolver = TestMessageResolverFactory.create();
        controller = new UserController(userService, recorder, messageResolver);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void createUserShouldReturnResponse() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setUsername("controller");
        request.setDisplayName("Controller User");
        request.setEmail("controller@demo.com");
        request.setPassword("password123");

        final ApiResponse<CreateUserResponse> response = controller.createUser(request);

        assertThat(response.getData().getUser().getUsername()).isEqualTo("controller");
        assertThat(response.getData().getUser().getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(response.getData().getActivation().getToken()).isNotBlank();
        assertThat(latestAudit().getAction()).isEqualTo("CREATE_USER");
    }

    @Test
    void activationEndpointsShouldActivateUser() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setUsername("to-activate");
        request.setDisplayName("To Activate");
        request.setEmail("activate@demo.com");
        request.setPassword("password123");
        final String userId = controller.createUser(request).getData().getUser().getId();

        final ActivationLinkResponse resend = controller.resendActivation(userId).getData();
        final ActivateUserRequest activateUserRequest = new ActivateUserRequest();
        activateUserRequest.setToken(resend.getToken());
        final ApiResponse<UserResponse> activated = controller.activate(activateUserRequest);

        assertThat(activated.getData().getStatus()).isEqualTo(UserStatus.ACTIVE);
        List<String> actions = auditService.query(new AuditQuery(null, null, null, null)).stream()
                .map(AuditLog::getAction)
                .collect(Collectors.toList());
        assertThat(actions).contains("RESEND_ACTIVATION", "ACTIVATE_USER");
    }

    @Test
    void assignRolesShouldUpdateUser() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setUsername("role-change");
        request.setDisplayName("Role Change");
        request.setEmail("role@demo.com");
        request.setPassword("password123");
        final String userId = controller.createUser(request).getData().getUser().getId();

        final AssignRolesRequest assign = new AssignRolesRequest();
        assign.setRoles(List.of("auditor"));

        final UserResponse response = controller.assignRoles(userId, assign).getData();

        assertThat(response.getRoles()).containsExactly("ROLE_AUDITOR");
        AuditLog audit = latestAudit();
        assertThat(audit.getAction()).isEqualTo("ASSIGN_ROLES");
    }

    private AuditLog latestAudit() {
        return auditService.query(new AuditQuery(null, null, null, null)).stream()
                .findFirst()
                .orElseThrow();
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
import java.util.Locale;
