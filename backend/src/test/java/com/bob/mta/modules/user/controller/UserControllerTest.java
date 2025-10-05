package com.bob.mta.modules.user.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.audit.service.AuditQuery;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.dto.ActivateUserRequest;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.AssignRolesRequest;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.dto.CreateUserResponse;
import com.bob.mta.modules.user.dto.UserResponse;
import com.bob.mta.modules.user.service.impl.UserServiceImpl;
import com.bob.mta.modules.user.support.FakeUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTest {

    private UserController controller;
    private FakeUserRepository repository;
    private UserServiceImpl userService;
    private InMemoryAuditService auditService;
    private MessageResolver messageResolver;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        repository = new FakeUserRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserServiceImpl(repository, passwordEncoder, new MutableClock(Instant.parse("2024-01-01T00:00:00Z")),
                Duration.ofHours(24));
        seedDefaultUsers();
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
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("controller");
        request.setDisplayName("Controller User");
        request.setEmail("controller@demo.com");
        request.setPassword("password123");

        ApiResponse<CreateUserResponse> response = controller.createUser(request);

        assertThat(response.getData().getUser().getUsername()).isEqualTo("controller");
        assertThat(response.getData().getUser().getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(response.getData().getActivation().getToken()).isNotBlank();
        assertThat(latestAudit().getAction()).isEqualTo("CREATE_USER");
    }

    @Test
    void activationEndpointsShouldActivateUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("to-activate");
        request.setDisplayName("To Activate");
        request.setEmail("activate@demo.com");
        request.setPassword("password123");
        String userId = controller.createUser(request).getData().getUser().getId();

        ActivationLinkResponse resend = controller.resendActivation(userId).getData();
        ActivateUserRequest activateUserRequest = new ActivateUserRequest();
        activateUserRequest.setToken(resend.getToken());
        ApiResponse<UserResponse> activated = controller.activate(activateUserRequest);

        assertThat(activated.getData().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(auditService.query(new AuditQuery(null, null, null, null)))
                .extracting(log -> log.getAction())
                .contains("RESEND_ACTIVATION", "ACTIVATE_USER");
    }

    @Test
    void assignRolesShouldUpdateUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("role-change");
        request.setDisplayName("Role Change");
        request.setEmail("role@demo.com");
        request.setPassword("password123");
        String userId = controller.createUser(request).getData().getUser().getId();

        AssignRolesRequest assign = new AssignRolesRequest();
        assign.setRoles(List.of("auditor"));

        UserResponse response = controller.assignRoles(userId, assign).getData();

        assertThat(response.getRoles()).containsExactly("ROLE_AUDITOR");
        assertThat(latestAudit().getAction()).isEqualTo("ASSIGN_ROLES");
    }

    private com.bob.mta.modules.audit.domain.AuditLog latestAudit() {
        return auditService.query(new AuditQuery(null, null, null, null)).stream()
                .findFirst()
                .orElseThrow();
    }

    private void seedDefaultUsers() {
        seedUser("admin", "系统管理员", "admin@example.com", "admin123", List.of("ROLE_ADMIN", "ROLE_OPERATOR"));
        seedUser("operator", "运维专员", "operator@example.com", "operator123", List.of("ROLE_OPERATOR"));
    }

    private void seedUser(String username, String displayName, String email, String password, List<String> roles) {
        User user = new User(UUID.randomUUID().toString(), username, displayName, email,
                passwordEncoder.encode(password), UserStatus.ACTIVE, new LinkedHashSet<>(roles));
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
    }
}
