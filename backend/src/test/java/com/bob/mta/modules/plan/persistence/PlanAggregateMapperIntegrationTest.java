package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import com.bob.mta.modules.plan.domain.PlanStatus;
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

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class PlanAggregateMapperIntegrationTest {

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
    private PlanAggregateMapper mapper;

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
    void resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE mt_plan_activity, mt_plan_node_attachment, mt_plan_node_execution, mt_plan_node, mt_plan_participant, mt_plan_reminder_rule, mt_plan CASCADE");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_node_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_reminder_id_seq RESTART WITH 1");
    }

    @Test
    void shouldFilterPlansByMultipleCriteriaAndPaginate() {
        OffsetDateTime now = OffsetDateTime.of(2024, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        insertPlan("PLAN-0001", "tenant-1", "customer-a", "owner-a", "Alpha Maintenance Window",
                "确认维护窗口与审批进度。", PlanStatus.SCHEDULED, now.plusHours(2), now.plusHours(5), now, "Asia/Tokyo");
        insertPlan("PLAN-0002", "tenant-1", "customer-a", "owner-b", "Beta Maintenance Review",
                "联合检查维护脚本。", PlanStatus.IN_PROGRESS, now.plusHours(4), now.plusHours(8), now, "Asia/Tokyo");
        insertPlan("PLAN-0003", "tenant-1", "customer-b", "owner-a", "Gamma Audit",
                "离线审计流程。", PlanStatus.CANCELED, now.plusHours(6), now.plusHours(10), now, "Asia/Tokyo");
        insertPlan("PLAN-0004", "tenant-2", "customer-a", "owner-a", "Delta Maintenance",
                "其他租户计划。", PlanStatus.SCHEDULED, now.plusHours(1), now.plusHours(3), now, "Asia/Tokyo");

        PlanQueryParameters firstPageFilter = new PlanQueryParameters(
                "tenant-1",
                "customer-a",
                null,
                "Maintenance",
                null,
                List.of(PlanStatus.SCHEDULED, PlanStatus.IN_PROGRESS),
                now.plusHours(1),
                now.plusHours(9),
                1,
                0,
                null
        );

        List<PlanEntity> page1 = mapper.findPlans(firstPageFilter);
        assertThat(page1).extracting(PlanEntity::id).containsExactly("PLAN-0001");
        assertThat(mapper.countPlans(firstPageFilter)).isEqualTo(2);

        PlanQueryParameters secondPageFilter = new PlanQueryParameters(
                firstPageFilter.tenantId(),
                firstPageFilter.customerId(),
                null,
                firstPageFilter.keyword(),
                null,
                firstPageFilter.statuses(),
                firstPageFilter.plannedStartFrom(),
                firstPageFilter.plannedEndTo(),
                firstPageFilter.limit(),
                1,
                null
        );

        List<PlanEntity> page2 = mapper.findPlans(secondPageFilter);
        assertThat(page2).extracting(PlanEntity::id).containsExactly("PLAN-0002");

        PlanQueryParameters ownerFilter = new PlanQueryParameters(
                firstPageFilter.tenantId(),
                firstPageFilter.customerId(),
                "owner-b",
                firstPageFilter.keyword(),
                PlanStatus.IN_PROGRESS,
                null,
                firstPageFilter.plannedStartFrom(),
                firstPageFilter.plannedEndTo(),
                null,
                null,
                null
        );

        List<PlanEntity> ownerScoped = mapper.findPlans(ownerFilter);
        assertThat(ownerScoped).extracting(PlanEntity::id).containsExactly("PLAN-0002");
    }

    @Test
    void shouldAggregateStatusAndOverdueMetrics() {
        OffsetDateTime now = OffsetDateTime.of(2024, 6, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        insertPlan("PLAN-1001", "tenant-analytics", "customer-a", "owner-a", "巡检准备",
                "准备巡检资料。", PlanStatus.SCHEDULED, now.minusHours(3), now.minusHours(1), now.minusHours(4), "Asia/Shanghai");
        insertPlan("PLAN-1002", "tenant-analytics", "customer-a", "owner-b", "系统维护",
                "维护中。", PlanStatus.IN_PROGRESS, now.minusHours(5), now.minusMinutes(30), now.minusHours(6), "Asia/Shanghai");
        insertPlan("PLAN-1003", "tenant-analytics", "customer-a", "owner-c", "总结复盘",
                "复盘结论。", PlanStatus.COMPLETED, now.minusDays(1), now.minusHours(12), now.minusDays(1), "Asia/Shanghai");
        insertPlan("PLAN-1004", "tenant-analytics", "customer-b", "owner-d", "其他客户计划",
                "不在统计范围。", PlanStatus.SCHEDULED, now, now.plusHours(2), now, "Asia/Shanghai");

        PlanAnalyticsQueryParameters analyticsParameters = new PlanAnalyticsQueryParameters(
                "tenant-analytics",
                "customer-a",
                null,
                now.minusDays(2),
                now.plusDays(1),
                List.of(PlanStatus.SCHEDULED, PlanStatus.IN_PROGRESS, PlanStatus.COMPLETED),
                now,
                5,
                5,
                5,
                now.plusHours(6)
        );

        Map<PlanStatus, Integer> statusTotals = mapper.countPlansByStatus(analyticsParameters).stream()
                .collect(Collectors.toMap(PlanStatusCountEntity::status, PlanStatusCountEntity::total));

        assertThat(statusTotals)
                .containsEntry(PlanStatus.SCHEDULED, 1)
                .containsEntry(PlanStatus.IN_PROGRESS, 1)
                .containsEntry(PlanStatus.COMPLETED, 1)
                .hasSize(3);

        long overdue = mapper.countOverduePlans(analyticsParameters);
        assertThat(overdue).isEqualTo(2L);
    }

    @Test
    void shouldSupportFullAggregateCrudOperations() {
        OffsetDateTime now = OffsetDateTime.of(2024, 7, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        String planId = "PLAN-CRUD-001";
        String rootNodeId = "NODE-ROOT-001";
        String childNodeId = "NODE-CHILD-001";
        String reminderId = "REM-001";

        PlanNode child = new PlanNode(childNodeId, "Review checklist", "TASK", "operator-b", 1,
                45, PlanNodeActionType.MANUAL, 80, "child-action", "Confirm readiness", List.of());
        PlanNode root = new PlanNode(rootNodeId, "Kickoff workflow", "TASK", "operator-a", 0,
                90, PlanNodeActionType.API_CALL, 100, "root-action", "Execute kickoff", List.of(child));

        PlanNodeExecution rootExecution = new PlanNodeExecution(rootNodeId, PlanNodeStatus.DONE,
                now.minusHours(3), now.minusHours(2), "operator-a", "completed", "root-log",
                List.of("file-root-1", "file-root-2"));
        PlanNodeExecution childExecution = new PlanNodeExecution(childNodeId, PlanNodeStatus.IN_PROGRESS,
                now.minusHours(1), null, "operator-b", "running", null,
                List.of("file-child-1"));

        PlanActivity activity = new PlanActivity(PlanActivityType.PLAN_CREATED, now.minusMinutes(30),
                "system", "plan.created", "ref-activity", Map.of("source", "crud-test"));

        PlanReminderRule reminderRule = new PlanReminderRule(reminderId, PlanReminderTrigger.BEFORE_START, 30,
                List.of("EMAIL", "SMS"), "template-1", List.of("operator-a", "operator-b"),
                "Primary reminder", true);
        PlanReminderPolicy reminderPolicy = new PlanReminderPolicy(List.of(reminderRule),
                now.minusMinutes(45), "scheduler-service");

        Plan plan = new Plan(planId, "tenant-crud", "Full CRUD validation", "Ensure mapper CRUD works",
                "customer-77", "owner-99", List.of("participant-1", "participant-2"), PlanStatus.SCHEDULED,
                now.plusHours(1), now.plusHours(5), now.minusDays(1), now.minusHours(10),
                "initial-reason", "owner-99", now.minusHours(11), "UTC",
                List.of(root), List.of(rootExecution, childExecution),
                now.minusDays(2), now.minusMinutes(10), List.of(activity), reminderPolicy);

        PlanAggregate aggregate = PlanPersistenceMapper.toAggregate(plan);

        mapper.insertPlan(aggregate.plan());
        mapper.insertParticipants(new java.util.ArrayList<>(aggregate.participants()));
        mapper.insertNodes(new java.util.ArrayList<>(aggregate.nodes()));
        mapper.insertExecutions(new java.util.ArrayList<>(aggregate.executions()));
        mapper.insertAttachments(new java.util.ArrayList<>(aggregate.attachments()));
        mapper.insertActivities(new java.util.ArrayList<>(aggregate.activities()));
        mapper.insertReminderRules(new java.util.ArrayList<>(aggregate.reminderRules()));

        PlanEntity persistedPlan = mapper.findPlanById(planId);
        assertThat(persistedPlan).isNotNull();
        assertThat(persistedPlan.title()).isEqualTo("Full CRUD validation");
        assertThat(persistedPlan.status()).isEqualTo(PlanStatus.SCHEDULED);

        List<PlanParticipantEntity> participants = mapper.findParticipantsByPlanIds(List.of(planId));
        assertThat(participants).extracting(PlanParticipantEntity::participantId)
                .containsExactlyInAnyOrder("participant-1", "participant-2");

        List<PlanNodeEntity> nodes = mapper.findNodesByPlanIds(List.of(planId));
        assertThat(nodes).extracting(PlanNodeEntity::nodeId)
                .containsExactly(rootNodeId, childNodeId);
        assertThat(nodes).filteredOn(node -> node.nodeId().equals(childNodeId))
                .single().satisfies(node -> assertThat(node.parentNodeId()).isEqualTo(rootNodeId));

        List<PlanNodeExecutionEntity> executions = mapper.findExecutionsByPlanIds(List.of(planId));
        assertThat(executions).hasSize(2);
        assertThat(executions).filteredOn(exec -> exec.nodeId().equals(rootNodeId))
                .single().satisfies(exec -> {
                    assertThat(exec.status()).isEqualTo(PlanNodeStatus.DONE);
                    assertThat(exec.operator()).isEqualTo("operator-a");
                    assertThat(exec.result()).isEqualTo("completed");
                });

        List<PlanNodeAttachmentEntity> attachments = mapper.findAttachmentsByPlanIds(List.of(planId));
        assertThat(attachments).extracting(PlanNodeAttachmentEntity::fileId)
                .containsExactlyInAnyOrder("file-root-1", "file-root-2", "file-child-1");

        List<PlanActivityEntity> activities = mapper.findActivitiesByPlanIds(List.of(planId));
        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).attributes()).containsEntry("source", "crud-test");

        List<PlanReminderRuleEntity> rules = mapper.findReminderRulesByPlanIds(List.of(planId));
        assertThat(rules).single().satisfies(rule -> {
            assertThat(rule.ruleId()).isEqualTo(reminderId);
            assertThat(rule.channels()).containsExactly("EMAIL", "SMS");
            assertThat(rule.recipients()).containsExactly("operator-a", "operator-b");
        });

        PlanEntity updatedEntity = new PlanEntity(planId, "tenant-crud", "customer-77", "owner-100",
                "Updated CRUD title", "Updated description", PlanStatus.IN_PROGRESS,
                plan.getPlannedStartTime(), plan.getPlannedEndTime(), plan.getActualStartTime(), plan.getActualEndTime(),
                "updated-reason", "owner-100", now.minusHours(1), "UTC",
                plan.getCreatedAt(), now, persistedPlan.reminderUpdatedAt(), persistedPlan.reminderUpdatedBy());
        mapper.updatePlan(updatedEntity);

        mapper.updateReminderAudit(planId, now, "auditor");

        PlanEntity updatedPlan = mapper.findPlanById(planId);
        assertThat(updatedPlan.owner()).isEqualTo("owner-100");
        assertThat(updatedPlan.title()).isEqualTo("Updated CRUD title");
        assertThat(updatedPlan.status()).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(updatedPlan.cancelReason()).isEqualTo("updated-reason");
        assertThat(updatedPlan.reminderUpdatedBy()).isEqualTo("auditor");
        assertThat(updatedPlan.reminderUpdatedAt()).isEqualTo(now);

        mapper.deleteAttachments(planId);
        mapper.deleteExecutions(planId);
        mapper.deleteNodes(planId);
        mapper.deleteParticipants(planId);
        mapper.deleteActivities(planId);
        mapper.deleteReminderRules(planId);
        mapper.deletePlan(planId);

        assertThat(mapper.findPlanById(planId)).isNull();
        assertThat(mapper.findParticipantsByPlanIds(List.of(planId))).isEmpty();
        assertThat(mapper.findNodesByPlanIds(List.of(planId))).isEmpty();
        assertThat(mapper.findExecutionsByPlanIds(List.of(planId))).isEmpty();
        assertThat(mapper.findAttachmentsByPlanIds(List.of(planId))).isEmpty();
        assertThat(mapper.findActivitiesByPlanIds(List.of(planId))).isEmpty();
        assertThat(mapper.findReminderRulesByPlanIds(List.of(planId))).isEmpty();
    }

    private void insertPlan(String planId,
                            String tenantId,
                            String customerId,
                            String ownerId,
                            String title,
                            String description,
                            PlanStatus status,
                            OffsetDateTime plannedStart,
                            OffsetDateTime plannedEnd,
                            OffsetDateTime createdAt,
                            String timezone) {
        jdbcTemplate.update(
                """
                        INSERT INTO mt_plan (plan_id, tenant_id, customer_id, owner_id, title, description, status,
                                             planned_start_time, planned_end_time, actual_start_time, actual_end_time,
                                             cancel_reason, canceled_by, canceled_at, timezone, created_at, updated_at,
                                             reminder_updated_at, reminder_updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, NULL, ?, ?, ?, NULL, NULL)
                        """,
                planId,
                tenantId,
                customerId,
                ownerId,
                title,
                description,
                status.name(),
                toTimestamp(plannedStart),
                toTimestamp(plannedEnd),
                timezone,
                toTimestamp(createdAt),
                toTimestamp(createdAt)
        );
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }
}
