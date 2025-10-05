package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import com.bob.mta.modules.plan.domain.PlanStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class PlanPersistenceAnalyticsRepositoryIntegrationTest {

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

    private static final String[] DROP_STATEMENTS = {
            "DROP TABLE IF EXISTS mt_plan_activity",
            "DROP TABLE IF EXISTS mt_plan_node_attachment",
            "DROP TABLE IF EXISTS mt_plan_node_execution",
            "DROP TABLE IF EXISTS mt_plan_node",
            "DROP TABLE IF EXISTS mt_plan_participant",
            "DROP TABLE IF EXISTS mt_plan_reminder_rule",
            "DROP TABLE IF EXISTS mt_plan",
            "DROP SEQUENCE IF EXISTS mt_plan_id_seq",
            "DROP SEQUENCE IF EXISTS mt_plan_node_id_seq",
            "DROP SEQUENCE IF EXISTS mt_plan_reminder_id_seq"
    };

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static final String[] CREATE_STATEMENTS = {
            "CREATE SEQUENCE IF NOT EXISTS mt_plan_id_seq START WITH 1",
            "CREATE SEQUENCE IF NOT EXISTS mt_plan_node_id_seq START WITH 1",
            "CREATE SEQUENCE IF NOT EXISTS mt_plan_reminder_id_seq START WITH 1",
            "CREATE TABLE IF NOT EXISTS mt_plan (" +
                    "plan_id VARCHAR(64) PRIMARY KEY, " +
                    "tenant_id VARCHAR(64), " +
                    "customer_id VARCHAR(64), " +
                    "owner_id VARCHAR(64), " +
                    "title VARCHAR(255) NOT NULL, " +
                    "description TEXT, " +
                    "status VARCHAR(32) NOT NULL, " +
                    "planned_start_time TIMESTAMPTZ, " +
                    "planned_end_time TIMESTAMPTZ, " +
                    "actual_start_time TIMESTAMPTZ, " +
                    "actual_end_time TIMESTAMPTZ, " +
                    "cancel_reason TEXT, " +
                    "canceled_by VARCHAR(64), " +
                    "canceled_at TIMESTAMPTZ, " +
                    "timezone VARCHAR(64), " +
                    "created_at TIMESTAMPTZ, " +
                    "updated_at TIMESTAMPTZ, " +
                    "reminder_updated_at TIMESTAMPTZ, " +
                    "reminder_updated_by VARCHAR(64))",
            "CREATE TABLE IF NOT EXISTS mt_plan_participant (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "participant_id VARCHAR(64) NOT NULL, " +
                    "PRIMARY KEY (plan_id, participant_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_node (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "node_id VARCHAR(64) NOT NULL, " +
                    "parent_node_id VARCHAR(64), " +
                    "name VARCHAR(255) NOT NULL, " +
                    "type VARCHAR(64) NOT NULL, " +
                    "assignee VARCHAR(64), " +
                    "order_index INT NOT NULL, " +
                    "expected_duration_minutes INT, " +
                    "action_type VARCHAR(64), " +
                    "completion_threshold INT, " +
                    "action_ref VARCHAR(255), " +
                    "description TEXT, " +
                    "PRIMARY KEY (plan_id, node_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_node_execution (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "node_id VARCHAR(64) NOT NULL, " +
                    "status VARCHAR(32) NOT NULL, " +
                    "start_time TIMESTAMPTZ, " +
                    "end_time TIMESTAMPTZ, " +
                    "operator_id VARCHAR(64), " +
                    "result_summary TEXT, " +
                    "execution_log TEXT, " +
                    "PRIMARY KEY (plan_id, node_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_node_attachment (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "node_id VARCHAR(64) NOT NULL, " +
                    "file_id VARCHAR(128) NOT NULL, " +
                    "PRIMARY KEY (plan_id, node_id, file_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_activity (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "activity_id VARCHAR(64) NOT NULL, " +
                    "activity_type VARCHAR(64) NOT NULL, " +
                    "occurred_at TIMESTAMPTZ NOT NULL, " +
                    "actor_id VARCHAR(64), " +
                    "message_key VARCHAR(255), " +
                    "reference_id VARCHAR(64), " +
                    "attributes JSONB, " +
                    "PRIMARY KEY (plan_id, activity_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_reminder_rule (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "rule_id VARCHAR(64) NOT NULL, " +
                    "trigger VARCHAR(64) NOT NULL, " +
                    "offset_minutes INT NOT NULL, " +
                    "channels JSONB, " +
                    "template_id VARCHAR(64), " +
                    "recipients JSONB, " +
                    "description TEXT, " +
                    "active BOOLEAN NOT NULL, " +
                    "PRIMARY KEY (plan_id, rule_id))"
    };

    private static final List<String> TABLES = List.of(
            "mt_plan_activity",
            "mt_plan_node_attachment",
            "mt_plan_node_execution",
            "mt_plan_node",
            "mt_plan_participant",
            "mt_plan_reminder_rule",
            "mt_plan"
    );

    @Autowired
    private PlanPersistencePlanRepository planRepository;

    @Autowired
    private PlanPersistenceAnalyticsRepository analyticsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void initializeSchema() {
        assertThat(dataSource).isNotNull();
        runStatements(DROP_STATEMENTS);
        runStatements(CREATE_STATEMENTS);
    }

    @BeforeEach
    void cleanDatabase() {
        TABLES.forEach(table -> jdbcTemplate.execute("DELETE FROM " + table));
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_node_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_reminder_id_seq RESTART WITH 1");
    }

    @Test
    void shouldAlignWithInMemoryAnalytics() throws Exception {
        OffsetDateTime reference = OffsetDateTime.of(2024, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        InMemoryPlanRepository memoryRepository = new InMemoryPlanRepository();

        Plan designPlan = new Plan(
                planRepository.nextPlanId(),
                "tenant-1",
                "Design Phase",
                "Drafting the engagement plan",
                "customer-1",
                "owner-alpha",
                List.of("participant-design"),
                PlanStatus.DESIGN,
                reference.minusDays(3),
                reference.minusDays(2),
                null,
                null,
                null,
                null,
                null,
                "UTC",
                List.of(),
                List.of(),
                reference.minusDays(5),
                reference.minusDays(3),
                List.of(new PlanActivity(PlanActivityType.PLAN_CREATED,
                        reference.minusDays(4),
                        "owner-alpha",
                        "plan.created",
                        "design-activity",
                        Map.of("phase", "design"))),
                PlanReminderPolicy.empty()
        );

        String scheduledRootNodeId = planRepository.nextNodeId();
        String scheduledChildNodeId = planRepository.nextNodeId();
        PlanNode scheduledChild = new PlanNode(
                scheduledChildNodeId,
                "Coordinate vendors",
                "TASK",
                "operator-2",
                1,
                90,
                PlanNodeActionType.EMAIL,
                100,
                null,
                "Confirm vendor availability",
                List.of()
        );
        PlanNode scheduledRoot = new PlanNode(
                scheduledRootNodeId,
                "Kickoff meeting",
                "TASK",
                "operator-1",
                0,
                60,
                PlanNodeActionType.REMOTE,
                100,
                null,
                "Prepare kickoff agenda",
                List.of(scheduledChild)
        );
        PlanNodeExecution scheduledRootExecution = new PlanNodeExecution(
                scheduledRootNodeId,
                PlanNodeStatus.DONE,
                reference.minusHours(2),
                reference.minusHours(1),
                "operator-1",
                "completed",
                null,
                List.of()
        );
        PlanNodeExecution scheduledChildExecution = new PlanNodeExecution(
                scheduledChildNodeId,
                PlanNodeStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
        PlanReminderRule reminderRule = new PlanReminderRule(
                planRepository.nextReminderId(),
                PlanReminderTrigger.BEFORE_PLAN_START,
                60,
                List.of("EMAIL", "SMS"),
                "template-start",
                List.of("owner-bravo"),
                "Kickoff reminder",
                true
        );
        PlanReminderPolicy scheduledPolicy = new PlanReminderPolicy(
                List.of(reminderRule),
                reference.minusDays(1),
                "system"
        );
        Plan scheduledPlan = new Plan(
                planRepository.nextPlanId(),
                "tenant-1",
                "Scheduled Deployment",
                "Coordinate the deployment activities",
                "customer-2",
                "owner-bravo",
                List.of("participant-bravo-1", "participant-bravo-2"),
                PlanStatus.SCHEDULED,
                reference.plusHours(1),
                reference.plusHours(3),
                null,
                null,
                null,
                null,
                null,
                "UTC",
                List.of(scheduledRoot),
                List.of(scheduledRootExecution, scheduledChildExecution),
                reference.minusDays(2),
                reference.minusHours(6),
                List.of(new PlanActivity(PlanActivityType.PLAN_UPDATED,
                        reference.minusHours(2),
                        "owner-bravo",
                        "plan.updated",
                        "scheduled-activity",
                        Map.of("status", "ready"))),
                scheduledPolicy
        );

        String inProgressNodeId = planRepository.nextNodeId();
        PlanNode inProgressNode = new PlanNode(
                inProgressNodeId,
                "Database migration",
                "TASK",
                "operator-3",
                0,
                120,
                PlanNodeActionType.REMOTE,
                100,
                null,
                "Execute migration steps",
                List.of()
        );
        PlanNodeExecution inProgressExecution = new PlanNodeExecution(
                inProgressNodeId,
                PlanNodeStatus.IN_PROGRESS,
                reference.minusHours(2),
                null,
                "operator-3",
                "running",
                null,
                List.of()
        );
        Plan inProgressPlan = new Plan(
                planRepository.nextPlanId(),
                "tenant-1",
                "Ongoing Migration",
                "Track live migration",
                "customer-3",
                "owner-bravo",
                List.of("participant-migration"),
                PlanStatus.IN_PROGRESS,
                reference.minusHours(6),
                reference.minusHours(1),
                reference.minusHours(6),
                null,
                null,
                null,
                null,
                "UTC",
                List.of(inProgressNode),
                List.of(inProgressExecution),
                reference.minusDays(3),
                reference.minusHours(1),
                List.of(),
                PlanReminderPolicy.empty()
        );

        Plan completedPlan = new Plan(
                planRepository.nextPlanId(),
                "tenant-1",
                "Completed Campaign",
                "Finished successfully",
                "customer-4",
                "owner-charlie",
                List.of("participant-complete"),
                PlanStatus.COMPLETED,
                reference.minusDays(7),
                reference.minusDays(6),
                reference.minusDays(7),
                reference.minusDays(6),
                null,
                null,
                null,
                "UTC",
                List.of(),
                List.of(),
                reference.minusDays(8),
                reference.minusDays(6),
                List.of(),
                PlanReminderPolicy.empty()
        );

        Plan canceledPlan = new Plan(
                planRepository.nextPlanId(),
                "tenant-1",
                "Canceled Initiative",
                "Stopped before kickoff",
                "customer-5",
                "owner-delta",
                List.of("participant-cancel"),
                PlanStatus.CANCELED,
                reference.plusDays(2),
                reference.plusDays(2).plusHours(3),
                null,
                null,
                "budget",
                "owner-delta",
                reference.minusDays(1),
                "UTC",
                List.of(),
                List.of(),
                reference.minusDays(4),
                reference.minusDays(1),
                List.of(),
                PlanReminderPolicy.empty()
        );

        persistPlan(designPlan, memoryRepository);
        persistPlan(scheduledPlan, memoryRepository);
        persistPlan(inProgressPlan, memoryRepository);
        persistPlan(completedPlan, memoryRepository);
        persistPlan(canceledPlan, memoryRepository);

        PlanAnalyticsQuery query = PlanAnalyticsQuery.builder()
                .tenantId("tenant-1")
                .from(reference.minusDays(10))
                .to(reference.plusDays(10))
                .referenceTime(reference)
                .upcomingLimit(5)
                .ownerLimit(5)
                .riskLimit(5)
                .dueSoonMinutes(240)
                .build();

        PlanAnalytics expected = new InMemoryPlanAnalyticsRepository(memoryRepository).summarize(query);
        PlanAnalytics actual = analyticsRepository.summarize(query);

        assertThat(actual.getTotalPlans()).isEqualTo(expected.getTotalPlans());
        assertThat(actual.getDesignCount()).isEqualTo(expected.getDesignCount());
        assertThat(actual.getScheduledCount()).isEqualTo(expected.getScheduledCount());
        assertThat(actual.getInProgressCount()).isEqualTo(expected.getInProgressCount());
        assertThat(actual.getCompletedCount()).isEqualTo(expected.getCompletedCount());
        assertThat(actual.getCanceledCount()).isEqualTo(expected.getCanceledCount());
        assertThat(actual.getOverdueCount()).isEqualTo(expected.getOverdueCount());

        assertUpcomingPlansAligned(expected.getUpcomingPlans(), actual.getUpcomingPlans());
        assertOwnerLoadsAligned(expected.getOwnerLoads(), actual.getOwnerLoads());
        assertRiskPlansAligned(expected.getRiskPlans(), actual.getRiskPlans());

        Map<String, Object> baseline = toBaseline(actual);
        if (Boolean.getBoolean("plan.analytics.printBaseline")) {
            System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(baseline));
        }
        Map<String, Object> fixture = MAPPER.readValue(
                new ClassPathResource("fixtures/plan-analytics-baseline.json").getInputStream(), Map.class);
        assertThat(baseline).isEqualTo(fixture);
    }

    private void assertUpcomingPlansAligned(List<PlanAnalytics.UpcomingPlan> expected,
                                            List<PlanAnalytics.UpcomingPlan> actual) {
        assertThat(actual).hasSameSizeAs(expected);
        for (int index = 0; index < expected.size(); index++) {
            PlanAnalytics.UpcomingPlan expectedPlan = expected.get(index);
            PlanAnalytics.UpcomingPlan actualPlan = actual.get(index);
            assertThat(actualPlan.getId()).isEqualTo(expectedPlan.getId());
            assertThat(actualPlan.getTitle()).isEqualTo(expectedPlan.getTitle());
            assertThat(actualPlan.getStatus()).isEqualTo(expectedPlan.getStatus());
            assertThat(actualPlan.getPlannedStartTime()).isEqualTo(expectedPlan.getPlannedStartTime());
            assertThat(actualPlan.getPlannedEndTime()).isEqualTo(expectedPlan.getPlannedEndTime());
            assertThat(actualPlan.getOwner()).isEqualTo(expectedPlan.getOwner());
            assertThat(actualPlan.getCustomerId()).isEqualTo(expectedPlan.getCustomerId());
            assertThat(actualPlan.getProgress()).isEqualTo(expectedPlan.getProgress());
        }
    }

    private void assertOwnerLoadsAligned(List<PlanAnalytics.OwnerLoad> expected,
                                         List<PlanAnalytics.OwnerLoad> actual) {
        assertThat(actual).hasSameSizeAs(expected);
        for (int index = 0; index < expected.size(); index++) {
            PlanAnalytics.OwnerLoad expectedLoad = expected.get(index);
            PlanAnalytics.OwnerLoad actualLoad = actual.get(index);
            assertThat(actualLoad.getOwnerId()).isEqualTo(expectedLoad.getOwnerId());
            assertThat(actualLoad.getTotalPlans()).isEqualTo(expectedLoad.getTotalPlans());
            assertThat(actualLoad.getActivePlans()).isEqualTo(expectedLoad.getActivePlans());
            assertThat(actualLoad.getOverduePlans()).isEqualTo(expectedLoad.getOverduePlans());
        }
    }

    private void assertRiskPlansAligned(List<PlanAnalytics.RiskPlan> expected,
                                        List<PlanAnalytics.RiskPlan> actual) {
        assertThat(actual).hasSameSizeAs(expected);
        for (int index = 0; index < expected.size(); index++) {
            PlanAnalytics.RiskPlan expectedRisk = expected.get(index);
            PlanAnalytics.RiskPlan actualRisk = actual.get(index);
            assertThat(actualRisk.getId()).isEqualTo(expectedRisk.getId());
            assertThat(actualRisk.getTitle()).isEqualTo(expectedRisk.getTitle());
            assertThat(actualRisk.getStatus()).isEqualTo(expectedRisk.getStatus());
            assertThat(actualRisk.getPlannedEndTime()).isEqualTo(expectedRisk.getPlannedEndTime());
            assertThat(actualRisk.getOwner()).isEqualTo(expectedRisk.getOwner());
            assertThat(actualRisk.getCustomerId()).isEqualTo(expectedRisk.getCustomerId());
            assertThat(actualRisk.getRiskLevel()).isEqualTo(expectedRisk.getRiskLevel());
            assertThat(actualRisk.getMinutesUntilDue()).isEqualTo(expectedRisk.getMinutesUntilDue());
            assertThat(actualRisk.getMinutesOverdue()).isEqualTo(expectedRisk.getMinutesOverdue());
        }
    }

    private Map<String, Object> toBaseline(PlanAnalytics analytics) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("totalPlans", analytics.getTotalPlans());
        root.put("designCount", analytics.getDesignCount());
        root.put("scheduledCount", analytics.getScheduledCount());
        root.put("inProgressCount", analytics.getInProgressCount());
        root.put("completedCount", analytics.getCompletedCount());
        root.put("canceledCount", analytics.getCanceledCount());
        root.put("overdueCount", analytics.getOverdueCount());
        root.put("upcomingPlans", analytics.getUpcomingPlans().stream()
                .map(plan -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", plan.getId());
                    item.put("title", plan.getTitle());
                    item.put("status", plan.getStatus() == null ? null : plan.getStatus().name());
                    item.put("plannedStartTime", plan.getPlannedStartTime() == null ? null : plan.getPlannedStartTime().toString());
                    item.put("plannedEndTime", plan.getPlannedEndTime() == null ? null : plan.getPlannedEndTime().toString());
                    item.put("owner", plan.getOwner());
                    item.put("customerId", plan.getCustomerId());
                    item.put("progress", plan.getProgress());
                    return item;
                })
                .toList());
        root.put("ownerLoads", analytics.getOwnerLoads().stream()
                .map(load -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("ownerId", load.getOwnerId());
                    item.put("totalPlans", load.getTotalPlans());
                    item.put("activePlans", load.getActivePlans());
                    item.put("overduePlans", load.getOverduePlans());
                    return item;
                })
                .toList());
        root.put("riskPlans", analytics.getRiskPlans().stream()
                .map(plan -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", plan.getId());
                    item.put("title", plan.getTitle());
                    item.put("status", plan.getStatus() == null ? null : plan.getStatus().name());
                    item.put("plannedEndTime", plan.getPlannedEndTime() == null ? null : plan.getPlannedEndTime().toString());
                    item.put("owner", plan.getOwner());
                    item.put("customerId", plan.getCustomerId());
                    item.put("riskLevel", plan.getRiskLevel() == null ? null : plan.getRiskLevel().name());
                    item.put("minutesUntilDue", plan.getMinutesUntilDue());
                    item.put("minutesOverdue", plan.getMinutesOverdue());
                    return item;
                })
                .toList());
        return root;
    }

    private void persistPlan(Plan plan, InMemoryPlanRepository memoryRepository) {
        planRepository.save(plan);
        planRepository.replaceTimeline(plan.getId(), plan.getActivities());
        planRepository.replaceReminderPolicy(plan.getId(), plan.getReminderPolicy());
        planRepository.replaceAttachments(plan.getId(), attachmentMap(plan));
        memoryRepository.save(plan);
    }

    private Map<String, List<String>> attachmentMap(Plan plan) {
        if (plan.getExecutions() == null || plan.getExecutions().isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> attachments = new LinkedHashMap<>();
        for (PlanNodeExecution execution : plan.getExecutions()) {
            if (execution.getFileIds() == null || execution.getFileIds().isEmpty()) {
                continue;
            }
            attachments.put(execution.getNodeId(), List.copyOf(execution.getFileIds()));
        }
        return attachments;
    }

    private void runStatements(String[] statements) {
        for (String statement : statements) {
            jdbcTemplate.execute(statement);
        }
    }
}
