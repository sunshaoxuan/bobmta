package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeAll
    void migrateDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE mt_plan_action_history, mt_plan_activity, mt_plan_node_attachment, mt_plan_node_execution, mt_plan_node, mt_plan_participant, mt_plan_reminder_rule, mt_plan CASCADE");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_node_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE mt_plan_reminder_id_seq RESTART WITH 1");
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void shouldFilterPlansByMultipleCriteriaAndPaginate() {
        OffsetDateTime now = OffsetDateTime.of(2024, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        insertPlan("PLAN-0001", "tenant-1", "customer-a", "owner-a", "Alpha Maintenance Window",
                "确认维护窗口与审批进度�?, PlanStatus.SCHEDULED, now.plusHours(2), now.plusHours(5), now, "Asia/Tokyo");
        insertPlan("PLAN-0002", "tenant-1", "customer-a", "owner-b", "Beta Maintenance Review",
                "联合检查维护脚本�?, PlanStatus.IN_PROGRESS, now.plusHours(4), now.plusHours(8), now, "Asia/Tokyo");
        insertPlan("PLAN-0003", "tenant-1", "customer-b", "owner-a", "Gamma Audit",
                "离线审计流程�?, PlanStatus.CANCELED, now.plusHours(6), now.plusHours(10), now, "Asia/Tokyo");
        insertPlan("PLAN-0004", "tenant-2", "customer-a", "owner-a", "Delta Maintenance",
                "其他租户计划�?, PlanStatus.SCHEDULED, now.plusHours(1), now.plusHours(3), now, "Asia/Tokyo");

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
    void shouldApplyKeywordStatusWindowAndExclusionFilters() {
        OffsetDateTime baseline = OffsetDateTime.of(2024, 5, 10, 6, 0, 0, 0, ZoneOffset.UTC);
        insertPlan("PLAN-KEY-1", "tenant-key", "customer-a", "owner-a", "巡检准备�?,
                "围绕巡检流程的前置确认�?, PlanStatus.SCHEDULED, baseline.plusHours(2), baseline.plusHours(4), baseline, "Asia/Shanghai");
        insertPlan("PLAN-KEY-EXCLUDE", "tenant-key", "customer-a", "owner-b", "应急巡检演练",
                "模拟巡检应急场景�?, PlanStatus.IN_PROGRESS, baseline.plusHours(3), baseline.plusHours(6), baseline, "Asia/Shanghai");
        insertPlan("PLAN-KEY-IGNORED", "tenant-key", "customer-a", "owner-a", "巡检取消案例",
                "演示取消状态�?, PlanStatus.CANCELED, baseline.plusHours(2), baseline.plusHours(5), baseline, "Asia/Shanghai");
        insertPlan("PLAN-KEY-OUTSIDE", "tenant-key", "customer-a", "owner-a", "巡检远期任务",
                "超出时间窗口的巡检�?, PlanStatus.SCHEDULED, baseline.plusHours(12), baseline.plusHours(15), baseline, "Asia/Shanghai");
        insertPlan("PLAN-KEY-OTHER", "tenant-other", "customer-a", "owner-a", "其他租户巡检",
                "验证租户隔离�?, PlanStatus.SCHEDULED, baseline.plusHours(3), baseline.plusHours(5), baseline, "Asia/Shanghai");

        PlanQueryParameters parameters = new PlanQueryParameters(
                "tenant-key",
                null,
                null,
                "巡检",
                null,
                List.of(PlanStatus.SCHEDULED, PlanStatus.IN_PROGRESS),
                baseline.plusHours(1),
                baseline.plusHours(8),
                null,
                null,
                "PLAN-KEY-EXCLUDE"
        );

        List<PlanEntity> plans = mapper.findPlans(parameters);
        assertThat(plans).extracting(PlanEntity::id).containsExactly("PLAN-KEY-1");
        assertThat(plans.get(0).title()).contains("巡检");
        assertThat(mapper.countPlans(parameters)).isEqualTo(1);
    }

    @Test
    void shouldAggregateStatusAndOverdueMetrics() {
        OffsetDateTime now = OffsetDateTime.of(2024, 6, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        insertPlan("PLAN-1001", "tenant-analytics", "customer-a", "owner-a", "巡检准备",
                "准备巡检资料�?, PlanStatus.SCHEDULED, now.minusHours(3), now.minusHours(1), now.minusHours(4), "Asia/Shanghai");
        insertPlan("PLAN-1002", "tenant-analytics", "customer-a", "owner-b", "系统维护",
                "维护中�?, PlanStatus.IN_PROGRESS, now.minusHours(5), now.minusMinutes(30), now.minusHours(6), "Asia/Shanghai");
        insertPlan("PLAN-1003", "tenant-analytics", "customer-a", "owner-c", "总结复盘",
                "复盘结论�?, PlanStatus.COMPLETED, now.minusDays(1), now.minusHours(12), now.minusDays(1), "Asia/Shanghai");
        insertPlan("PLAN-1004", "tenant-analytics", "customer-b", "owner-d", "其他客户计划",
                "不在统计范围�?, PlanStatus.SCHEDULED, now, now.plusHours(2), now, "Asia/Shanghai");

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
    void shouldReturnUpcomingRiskAndOwnerWorkloads() {
        OffsetDateTime reference = OffsetDateTime.of(2024, 7, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        insertPlan("PLAN-UPCOMING", "tenant-metrics", "customer-x", "owner-1", "未来巡检窗口",
                "规划即将到来的巡检�?, PlanStatus.SCHEDULED, reference.plusMinutes(30), reference.plusHours(3), reference.minusDays(1), "Asia/Shanghai");
        insertPlan("PLAN-DUE-SOON", "tenant-metrics", "customer-x", "owner-2", "待结束维�?,
                "确认维护收尾�?, PlanStatus.IN_PROGRESS, reference.plusMinutes(60), reference.plusMinutes(90), reference.minusDays(2), "Asia/Shanghai");
        insertPlan("PLAN-OVERDUE", "tenant-metrics", "customer-x", "owner-2", "超时处理",
                "跟进延迟任务�?, PlanStatus.IN_PROGRESS, reference.minusHours(4), reference.minusMinutes(30), reference.minusDays(2), "Asia/Shanghai");
        insertPlan("PLAN-IGNORED", "tenant-metrics", "customer-y", "owner-3", "其他客户计划",
                "不在当前客户范围�?, PlanStatus.SCHEDULED, reference.plusMinutes(45), reference.plusHours(2), reference, "Asia/Shanghai");

        insertNodeExecution("PLAN-DUE-SOON", "NODE-1001", PlanNodeStatus.DONE, reference.minusHours(1), reference.minusMinutes(30), "user-a", "收尾完成", "log-done");
        insertNodeExecution("PLAN-DUE-SOON", "NODE-1002", PlanNodeStatus.IN_PROGRESS, reference.minusMinutes(20), null, "user-b", "继续执行", "log-progress");
        insertNodeExecution("PLAN-OVERDUE", "NODE-2001", PlanNodeStatus.IN_PROGRESS, reference.minusHours(2), null, "user-c", "等待复盘", "log-overdue");

        PlanAnalyticsQueryParameters parameters = new PlanAnalyticsQueryParameters(
                "tenant-metrics",
                "customer-x",
                null,
                reference.minusDays(1),
                reference.plusDays(1),
                List.of(PlanStatus.SCHEDULED, PlanStatus.IN_PROGRESS),
                reference,
                5,
                5,
                5,
                reference.plusHours(2)
        );

        List<PlanUpcomingPlanEntity> upcoming = mapper.findUpcomingPlans(parameters);
        assertThat(upcoming).extracting(PlanUpcomingPlanEntity::planId)
                .containsExactly("PLAN-UPCOMING", "PLAN-DUE-SOON");
        PlanUpcomingPlanEntity dueSoonPlan = upcoming.stream()
                .filter(plan -> plan.planId().equals("PLAN-DUE-SOON"))
                .findFirst()
                .orElseThrow();
        assertThat(dueSoonPlan.completedNodes()).isEqualTo(1L);
        assertThat(dueSoonPlan.totalNodes()).isEqualTo(2L);

        List<PlanOwnerLoadEntity> ownerLoads = mapper.findOwnerLoads(parameters);
        assertThat(ownerLoads).extracting(PlanOwnerLoadEntity::ownerId)
                .containsExactly("owner-2", "owner-1");
        assertThat(ownerLoads.get(0).totalPlans()).isEqualTo(2L);
        assertThat(ownerLoads.get(0).activePlans()).isEqualTo(2L);
        assertThat(ownerLoads.get(0).overduePlans()).isEqualTo(1L);

        List<PlanRiskPlanEntity> riskPlans = mapper.findRiskPlans(parameters);
        assertThat(riskPlans).extracting(PlanRiskPlanEntity::planId)
                .containsExactly("PLAN-OVERDUE", "PLAN-DUE-SOON");
        assertThat(riskPlans.get(0).riskLevel()).isEqualTo("OVERDUE");
        assertThat(riskPlans.get(0).minutesOverdue()).isEqualTo(30L);
        assertThat(riskPlans.get(1).riskLevel()).isEqualTo("DUE_SOON");
        assertThat(riskPlans.get(1).minutesUntilDue()).isEqualTo(90L);
    }

    @Test
    void shouldLoadPlanAggregateGraph() {
        OffsetDateTime now = OffsetDateTime.of(2024, 8, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        insertPlan("PLAN-GRAPH", "tenant-graph", "customer-a", "owner-z", "图谱计划",
                "验证聚合查询�?, PlanStatus.IN_PROGRESS, now.minusHours(1), now.plusHours(5), now.minusDays(1), "Asia/Shanghai");

        insertParticipant("PLAN-GRAPH", "user-1001");
        insertParticipant("PLAN-GRAPH", "user-1002");

        insertNode("PLAN-GRAPH", "NODE-A", null, "准备阶段", "TASK", "user-1001", 0, 60, PlanNodeActionType.MANUAL, 100, "checklist-a", "确认预检项目");
        insertNode("PLAN-GRAPH", "NODE-B", "NODE-A", "执行阶段", "TASK", "user-1002", 1, 45, PlanNodeActionType.API_CALL, 80, "workflow-b", "调用自动化脚�?);

        insertNodeExecution("PLAN-GRAPH", "NODE-A", PlanNodeStatus.DONE, now.minusHours(2), now.minusHours(1), "user-1001", "完成预检", "log-node-a");
        insertNodeExecution("PLAN-GRAPH", "NODE-B", PlanNodeStatus.IN_PROGRESS, now.minusMinutes(30), null, "user-1002", "正在执行", "log-node-b");

        insertAttachment("PLAN-GRAPH", "NODE-A", "file-precheck-report");
        insertAttachment("PLAN-GRAPH", "NODE-B", "file-automation-log");

        insertActivity("PLAN-GRAPH", "ACT-1", PlanActivityType.PLAN_UPDATED, now.minusMinutes(40), "user-1001", "plan.activity.updated", "NODE-A", "{\"field\":\"value\"}");
        insertActivity("PLAN-GRAPH", "ACT-2", PlanActivityType.NODE_COMPLETED, now.minusMinutes(20), "user-1001", "plan.activity.nodeCompleted", "NODE-A", "{\"result\":\"ok\"}");

        insertReminderRule("PLAN-GRAPH", "REM-1", PlanReminderTrigger.BEFORE_PLAN_START, 30, "[\"EMAIL\",\"IM\"]", "tmpl-plan", "[\"owner-z\"]", "开工提�?, true);

        PlanEntity plan = mapper.findPlanById("PLAN-GRAPH");
        assertThat(plan).isNotNull();
        assertThat(plan.title()).isEqualTo("图谱计划");
        assertThat(plan.status()).isEqualTo(PlanStatus.IN_PROGRESS);

        List<String> planIds = List.of("PLAN-GRAPH");

        List<PlanParticipantEntity> participants = mapper.findParticipantsByPlanIds(planIds);
        assertThat(participants).extracting(PlanParticipantEntity::participantId)
                .containsExactly("user-1001", "user-1002");

        List<PlanNodeEntity> nodes = mapper.findNodesByPlanIds(planIds);
        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(0).actionType()).isEqualTo(PlanNodeActionType.MANUAL);
        assertThat(nodes.get(1).parentNodeId()).isEqualTo("NODE-A");

        List<PlanNodeExecutionEntity> executions = mapper.findExecutionsByPlanIds(planIds);
        assertThat(executions).hasSize(2);
        assertThat(executions).extracting(PlanNodeExecutionEntity::status)
                .containsExactly(PlanNodeStatus.DONE, PlanNodeStatus.IN_PROGRESS);

        List<PlanNodeAttachmentEntity> attachments = mapper.findAttachmentsByPlanIds(planIds);
        assertThat(attachments).extracting(PlanNodeAttachmentEntity::fileId)
                .containsExactlyInAnyOrder("file-precheck-report", "file-automation-log");

        List<PlanActivityEntity> activities = mapper.findActivitiesByPlanIds(planIds);
        assertThat(activities).extracting(PlanActivityEntity::type)
                .containsExactly(PlanActivityType.PLAN_UPDATED, PlanActivityType.NODE_COMPLETED);
        assertThat(activities.get(0).attributes()).containsEntry("field", "value");

        List<PlanReminderRuleEntity> reminderRules = mapper.findReminderRulesByPlanIds(planIds);
        assertThat(reminderRules).hasSize(1);
        PlanReminderRuleEntity reminderRule = reminderRules.get(0);
        assertThat(reminderRule.trigger()).isEqualTo(PlanReminderTrigger.BEFORE_PLAN_START);
        assertThat(reminderRule.channels()).containsExactlyInAnyOrder("EMAIL", "IM");
        assertThat(reminderRule.recipients()).containsExactly("owner-z");
    }

    @Test
    void shouldRollbackPlanWriteWhenTransactionFails() {
        OffsetDateTime now = OffsetDateTime.of(2024, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        String planId = "PLAN-TX-001";

        PlanEntity plan = new PlanEntity(
                planId,
                "tenant-tx",
                "customer-z",
                "owner-zeta",
                "事务回滚验证",
                "当事务失败时不应残留部分写入�?,
                PlanStatus.SCHEDULED,
                now.plusHours(2),
                now.plusHours(5),
                null,
                null,
                null,
                null,
                null,
                "Asia/Shanghai",
                now.minusDays(1),
                now.minusDays(1),
                null,
                null
        );

        PlanParticipantEntity participant = new PlanParticipantEntity(planId, "user-rollback");
        PlanNodeEntity node = new PlanNodeEntity(planId, "NODE-TX-1", null, "校验节点", "TASK", "user-rollback", 0,
                30, PlanNodeActionType.MANUAL, 100, "tx-check", "确保步骤具备幂等�?);
        PlanNodeExecutionEntity execution = new PlanNodeExecutionEntity(planId, "NODE-TX-1", PlanNodeStatus.IN_PROGRESS,
                now.minusHours(1), null, "user-rollback", "开始执�?, "log-tx");
        PlanNodeAttachmentEntity attachment = new PlanNodeAttachmentEntity(planId, "NODE-TX-1", "file-tx-proof");
        PlanActivityEntity activity = new PlanActivityEntity(planId, "ACT-TX-1", PlanActivityType.PLAN_UPDATED,
                now.minusMinutes(30), "owner-zeta", "plan.activity.planUpdated", "NODE-TX-1", Map.of("field", "value"));
        PlanReminderRuleEntity reminderRule = new PlanReminderRuleEntity(planId, "REM-TX-1",
                PlanReminderTrigger.BEFORE_PLAN_START, 45, List.of("EMAIL"), "tmpl-tx",
                List.of("owner-zeta"), "事务测试提醒", true);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            mapper.insertPlan(plan);
            mapper.insertParticipants(new ArrayList<>(List.of(participant)));
            mapper.insertNodes(new ArrayList<>(List.of(node)));
            mapper.insertExecutions(new ArrayList<>(List.of(execution)));
            mapper.insertAttachments(new ArrayList<>(List.of(attachment)));
            mapper.insertActivities(new ArrayList<>(List.of(activity)));
            mapper.insertReminderRules(new ArrayList<>(List.of(reminderRule)));
            throw new RuntimeException("simulate failure");
        })).isInstanceOf(RuntimeException.class);

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

    private void insertParticipant(String planId, String participantId) {
        jdbcTemplate.update(
                "INSERT INTO mt_plan_participant (plan_id, participant_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                planId,
                participantId
        );
    }

    private void insertNode(String planId,
                            String nodeId,
                            String parentNodeId,
                            String name,
                            String type,
                            String assignee,
                            int orderIndex,
                            Integer expectedDuration,
                            PlanNodeActionType actionType,
                            Integer completionThreshold,
                            String actionRef,
                            String description) {
        jdbcTemplate.update(
                """
                        INSERT INTO mt_plan_node (plan_id, node_id, parent_node_id, name, type, assignee,
                                                   order_index, expected_duration_minutes, action_type,
                                                   completion_threshold, action_ref, description)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                planId,
                nodeId,
                parentNodeId,
                name,
                type,
                assignee,
                orderIndex,
                expectedDuration,
                actionType.name(),
                completionThreshold,
                actionRef,
                description
        );
    }

    private void insertNodeExecution(String planId,
                                     String nodeId,
                                     PlanNodeStatus status,
                                     OffsetDateTime startTime,
                                     OffsetDateTime endTime,
                                     String operator,
                                     String result,
                                     String log) {
        jdbcTemplate.update(
                """
                        INSERT INTO mt_plan_node_execution (plan_id, node_id, status, start_time, end_time, operator_id,
                                                            result_summary, execution_log)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                planId,
                nodeId,
                status.name(),
                toTimestamp(startTime),
                toTimestamp(endTime),
                operator,
                result,
                log
        );
    }

    private void insertAttachment(String planId, String nodeId, String fileId) {
        jdbcTemplate.update(
                "INSERT INTO mt_plan_node_attachment (plan_id, node_id, file_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                planId,
                nodeId,
                fileId
        );
    }

    private void insertActivity(String planId,
                                String activityId,
                                PlanActivityType type,
                                OffsetDateTime occurredAt,
                                String actor,
                                String messageKey,
                                String referenceId,
                                String attributesJson) {
        jdbcTemplate.update(
                """
                        INSERT INTO mt_plan_activity (plan_id, activity_id, activity_type, occurred_at, actor_id,
                                                      message_key, reference_id, attributes)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                        """,
                planId,
                activityId,
                type.name(),
                toTimestamp(occurredAt),
                actor,
                messageKey,
                referenceId,
                attributesJson
        );
    }

    private void insertReminderRule(String planId,
                                    String ruleId,
                                    PlanReminderTrigger trigger,
                                    int offsetMinutes,
                                    String channelsJson,
                                    String templateId,
                                    String recipientsJson,
                                    String description,
                                    boolean active) {
        jdbcTemplate.update(
                """
                        INSERT INTO mt_plan_reminder_rule (plan_id, rule_id, trigger, offset_minutes, channels,
                                                           template_id, recipients, description, active)
                        VALUES (?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, ?, ?)
                        """,
                planId,
                ruleId,
                trigger.name(),
                offsetMinutes,
                channelsJson,
                templateId,
                recipientsJson,
                description,
                active
        );
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }
}
