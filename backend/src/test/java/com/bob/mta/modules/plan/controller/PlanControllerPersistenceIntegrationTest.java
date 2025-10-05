package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.plan.dto.PlanBoardResponse;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import com.bob.mta.modules.plan.service.impl.PersistencePlanService;
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
class PlanControllerPersistenceIntegrationTest {

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
    private PlanController controller;

    @Autowired
    private PlanService planService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @BeforeAll
    void migrateDatabase() {
        flyway.clean();
        flyway.migrate();
        assertThat(planService).isInstanceOf(PersistencePlanService.class);
    }

    @BeforeEach
    void resetData() {
        jdbcTemplate.execute("TRUNCATE TABLE mt_plan_action_history, mt_plan_activity, mt_plan_node_attachment, mt_plan_node_execution, mt_plan_node, mt_plan_participant, mt_plan_reminder_rule, mt_plan CASCADE");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_node_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_reminder_id_seq RESTART WITH 1");
    }

    @Test
    void listEndpointShouldReturnPersistedPlan() {
        OffsetDateTime start = OffsetDateTime.of(2024, 6, 1, 1, 0, 0, 0, ZoneOffset.UTC);
        PlanNodeCommand node = new PlanNodeCommand(null, "¼ì²éÉè±¸", "CHECK", "alice", 1,
                30, null, 100, null, "", List.of());
        CreatePlanCommand command = new CreatePlanCommand("tenant-1", "ÁùÔÂÑ²¼ì", "ÀýÐÐÑ²¼ì", "cust-1", "alice",
                start, start.plusHours(2), "Asia/Tokyo", List.of("alice"), List.of(node));
        planService.createPlan(command);

        ApiResponse<PageResponse<PlanSummaryResponse>> response = controller.list(
                "tenant-1", null, null, null, null, null, null, 1, 10);
        assertThat(response.getData().getTotal()).isEqualTo(1);
        assertThat(response.getData().getItems())
                .extracting(PlanSummaryResponse::title)
                .containsExactly("ÁùÔÂÑ²¼ì");

        ApiResponse<PlanBoardResponse> board = controller.board(
                "tenant-1", null, null, null, null, null, null, null, null, null);
        assertThat(board.getData().metrics().totalPlans()).isEqualTo(1);

        String planId = response.getData().getItems().getFirst().id();
        ApiResponse<PlanDetailResponse> detail = controller.detail(planId);
        assertThat(detail.getData().nodes()).hasSize(1);
    }
}
