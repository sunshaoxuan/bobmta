package com.bob.mta.modules.user.persistence;

import com.bob.mta.modules.user.domain.ActivationToken;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class PersistenceUserRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bobmta")
            .withUsername("bobmta")
            .withPassword("secret");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @BeforeAll
    void migrateDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE mt_user_activation_token, mt_user_role, mt_user CASCADE");
        jdbcTemplate.execute("ALTER SEQUENCE mt_user_id_seq RESTART WITH 1");
    }

    @Test
    void shouldCreateAndLoadUserWithRolesAndActivation() {
        User pending = new User("user-1", "dbuser", "数据库用户", "dbuser@example.com", "hashed",
                UserStatus.PENDING_ACTIVATION, new LinkedHashSet<>(List.of("ROLE_ADMIN", "ROLE_OPERATOR")));
        ActivationToken token = new ActivationToken("token-1", Instant.now().plusSeconds(3600));
        pending.issueActivationToken(token);

        userRepository.create(pending);

        User loaded = userRepository.findById("user-1").orElseThrow();
        assertThat(loaded.getRoles()).containsExactly("ROLE_ADMIN", "ROLE_OPERATOR");
        assertThat(loaded.getActivationToken()).isNotNull();
        assertThat(userRepository.findByUsername("DBUSER")).isPresent();
        assertThat(userRepository.findByActivationToken(token.token())).isPresent();
    }

    @Test
    void shouldReplaceRolesAndRemoveActivation() {
        User user = new User("user-2", "role-user", "角色用户", "role@example.com", "hash",
                UserStatus.PENDING_ACTIVATION, new LinkedHashSet<>(List.of("ROLE_OPERATOR")));
        ActivationToken token = new ActivationToken("token-2", Instant.now().plusSeconds(7200));
        user.issueActivationToken(token);
        userRepository.create(user);

        Set<String> newRoles = new LinkedHashSet<>(List.of("ROLE_AUDITOR", "ROLE_VIEWER"));
        userRepository.replaceRoles("user-2", newRoles);

        User reloaded = userRepository.findById("user-2").orElseThrow();
        assertThat(reloaded.getRoles()).containsExactly("ROLE_AUDITOR", "ROLE_VIEWER");

        userRepository.deleteActivationToken("user-2");
        User withoutToken = userRepository.findById("user-2").orElseThrow();
        assertThat(withoutToken.getActivationToken()).isNull();

        user.activate();
        userRepository.update(user);
        List<User> activeUsers = userRepository.findAll(UserStatus.ACTIVE);
        assertThat(activeUsers).extracting(User::getUsername).contains("role-user");
    }
}
