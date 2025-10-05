package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanSearchCriteria;
import com.bob.mta.modules.plan.service.PlanBoardView;
import com.bob.mta.modules.plan.service.PlanSearchResult;
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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class PersistencePlanServiceIntegrationTest {

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
    private PersistencePlanService planService;

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
        jdbcTemplate.execute("TRUNCATE TABLE mt_plan_activity, mt_plan_node_attachment, mt_plan_node_execution, mt_plan_node, mt_plan_participant, mt_plan_reminder_rule, mt_plan CASCADE");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_node_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_reminder_id_seq RESTART WITH 1");
    }

    @Test
    void shouldPersistPlanLifecycle() {
        OffsetDateTime now = OffsetDateTime.of(2024, 5, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        PlanNodeCommand rootNode = new PlanNodeCommand(null, "Initial Check", "INSPECTION", "alice", 1,
                60, null, 100, null, "Inspect racks", List.of());
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-1",
                "Data Center Visit",
                "Routine inspection",
                "cust-1",
                "alice",
                now.plusHours(1),
                now.plusHours(3),
                "Asia/Tokyo",
                List.of("alice", "bob"),
                List.of(rootNode)
        );

        Plan created = planService.createPlan(command);
        assertThat(created.getId()).isNotBlank();
        assertThat(created.getStatus()).isEqualTo(PlanStatus.DESIGN);

        Plan published = planService.publishPlan(created.getId(), "alice");
        assertThat(published.getStatus()).isEqualTo(PlanStatus.SCHEDULED);

        PlanSearchResult result = planService.listPlans("tenant-1", null, null, null, null,
                now, now.plusDays(1), 1, 10);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.plans()).extracting(Plan::getId).containsExactly(created.getId());

        PlanBoardView board = planService.getPlanBoard(PlanSearchCriteria.builder()
                        .tenantId("tenant-1")
                        .build(),
                null);
        assertThat(board.metrics().totalPlans()).isEqualTo(1);

        Plan fetched = planService.getPlan(created.getId());
        assertThat(fetched.getNodes()).hasSize(1);
        assertThat(planService.getPlanTimeline(created.getId())).isNotEmpty();
    }
}
