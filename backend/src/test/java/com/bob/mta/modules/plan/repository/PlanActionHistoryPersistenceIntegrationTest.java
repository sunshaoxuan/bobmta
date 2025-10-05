package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanActionHistory;
import com.bob.mta.modules.plan.domain.PlanActionStatus;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class PlanActionHistoryPersistenceIntegrationTest {

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
    private PlanService planService;

    @Autowired
    private PlanActionHistoryRepository actionHistoryRepository;

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
    void resetData() {
        jdbcTemplate.execute("TRUNCATE TABLE mt_plan_action_history, mt_plan_activity, mt_plan_node_attachment, mt_plan_node_execution, mt_plan_node, mt_plan_participant, mt_plan_reminder_rule, mt_plan CASCADE");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_node_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_reminder_id_seq RESTART WITH 1");
    }

    @Test
    void appendAndRetrieveHistoryShouldPersistAcrossSessions() {
        OffsetDateTime now = OffsetDateTime.of(2024, 7, 1, 1, 0, 0, 0, ZoneOffset.UTC);
        PlanNodeCommand node = new PlanNodeCommand(null, "Remote Prep", "REMOTE", "alice", 1,
                60, PlanNodeActionType.REMOTE, 100, "remote-template-1", "Prepare remote session", List.of());
        CreatePlanCommand command = new CreatePlanCommand("tenant-1", "Remote maintenance", "desc",
                "cust-1", "alice", now, now.plusHours(2), "Asia/Tokyo", List.of("alice"), List.of(node));

        var created = planService.createPlan(command);
        assertThat(created.getStatus()).isEqualTo(PlanStatus.DESIGN);
        String nodeId = created.getNodes().getFirst().getId();

        String historyId = UUID.randomUUID().toString();
        PlanActionHistory history = new PlanActionHistory(
                historyId,
                created.getId(),
                nodeId,
                PlanNodeActionType.REMOTE,
                "remote-template-1",
                now,
                "alice",
                PlanActionStatus.SUCCESS,
                "plan.action.remoteReady",
                null,
                Map.of("operator", "alice"),
                Map.of("endpoint", "https://ops.example.com/session/123", "attempts", "1")
        );

        actionHistoryRepository.append(history);

        List<PlanActionHistory> stored = actionHistoryRepository.findByPlanId(created.getId());
        assertThat(stored).hasSize(1);
        PlanActionHistory persisted = stored.getFirst();
        assertThat(persisted.getId()).isEqualTo(historyId);
        assertThat(persisted.getContext()).containsEntry("operator", "alice");
        assertThat(persisted.getMetadata()).containsEntry("endpoint", "https://ops.example.com/session/123");

        planService.deletePlan(created.getId());
        assertThat(actionHistoryRepository.findByPlanId(created.getId())).isEmpty();
    }
}
