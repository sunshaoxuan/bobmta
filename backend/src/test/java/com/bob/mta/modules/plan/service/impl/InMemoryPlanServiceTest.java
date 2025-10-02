package com.bob.mta.modules.plan.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanActionHistory;
import com.bob.mta.modules.plan.domain.PlanActionStatus;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderSchedule;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.repository.InMemoryPlanActionHistoryRepository;
import com.bob.mta.modules.plan.repository.InMemoryPlanAnalyticsRepository;
import com.bob.mta.modules.plan.repository.PlanBoardQuery;
import com.bob.mta.modules.plan.repository.InMemoryPlanRepository;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import com.bob.mta.modules.plan.service.PlanBoardView;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.template.domain.RenderedTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class InMemoryPlanServiceTest {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();
    private final InMemoryPlanAnalyticsRepository analyticsRepository = new InMemoryPlanAnalyticsRepository(repository);
    private final InMemoryPlanActionHistoryRepository actionHistoryRepository = new InMemoryPlanActionHistoryRepository();
    private final TestTemplateService templateService = new TestTemplateService();
    private final RecordingNotificationGateway notificationGateway = new RecordingNotificationGateway();
    private final MessageResolver messageResolver = TestMessageResolverFactory.create();
    private final InMemoryPlanService service = new InMemoryPlanService(new InMemoryFileService(), repository,
            analyticsRepository, actionHistoryRepository, templateService, notificationGateway, messageResolver);

    @BeforeEach
    void setUpLocale() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("getPlanBoard aggregates customer groups and time buckets")
    void shouldAggregatePlanBoardView() {
        OffsetDateTime base = OffsetDateTime.parse("2024-04-01T08:00:00+08:00");
        CreatePlanCommand customerA = new CreatePlanCommand(
                "tenant-board",
                "客户A巡检",
                "客户A巡检任务",
                "cust-board-a",
                "board-owner",
                base.plusHours(2),
                base.plusHours(5),
                "Asia/Shanghai",
                List.of("board-owner"),
                List.of(new PlanNodeCommand(null, "检查列表", "CHECKLIST", "board-owner", 1, 60,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        CreatePlanCommand customerB = new CreatePlanCommand(
                "tenant-board",
                "客户B巡检",
                "客户B巡检任务",
                "cust-board-b",
                "board-owner",
                base.plusDays(1).plusHours(1),
                base.plusDays(1).plusHours(4),
                "Asia/Shanghai",
                List.of("board-owner"),
                List.of(new PlanNodeCommand(null, "巡检步骤", "CHECKLIST", "board-owner", 1, 90,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );

        var firstPlan = service.createPlan(customerA);
        var secondPlan = service.createPlan(customerB);
        service.publishPlan(firstPlan.getId(), "board-owner");
        service.publishPlan(secondPlan.getId(), "board-owner");

        PlanBoardQuery query = PlanBoardQuery.builder()
                .tenantId("tenant-board")
                .granularity(PlanBoardQuery.TimeGranularity.DAY)
                .from(base.minusDays(1))
                .to(base.plusDays(2))
                .build();

        PlanBoardView board = service.getPlanBoard(query);

        assertThat(board.getMetrics().getTotalPlans()).isEqualTo(2);
        assertThat(board.getCustomerGroups())
                .extracting(PlanBoardView.CustomerGroup::getCustomerId)
                .containsExactlyInAnyOrder("cust-board-a", "cust-board-b");
        assertThat(board.getTimeBuckets()).hasSize(2);
        assertThat(board.getTimeBuckets())
                .extracting(PlanBoardView.TimeBucket::getBucketId)
                .contains(base.toLocalDate().toString(), base.plusDays(1).toLocalDate().toString());
    }

    @Test
    @DisplayName("getPlanBoard respects tenant and customer filters")
    void shouldFilterPlanBoardByTenantAndCustomers() {
        OffsetDateTime baseline = OffsetDateTime.parse("2024-05-01T09:00:00+08:00");
        CreatePlanCommand tenantPlan = new CreatePlanCommand(
                "tenant-board-filter",
                "租户内计划",
                "tenant scoped",
                "cust-filter-a",
                "filter-owner",
                baseline.plusHours(1),
                baseline.plusHours(3),
                "Asia/Shanghai",
                List.of("filter-owner"),
                List.of(new PlanNodeCommand(null, "准备工作", "CHECKLIST", "filter-owner", 1, 45,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        CreatePlanCommand ignoredTenantPlan = new CreatePlanCommand(
                "tenant-board-other",
                "其他租户计划",
                "should be filtered",
                "cust-filter-b",
                "filter-owner",
                baseline.plusHours(2),
                baseline.plusHours(5),
                "Asia/Shanghai",
                List.of("filter-owner"),
                List.of(new PlanNodeCommand(null, "忽略节点", "CHECKLIST", "filter-owner", 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );

        var scoped = service.createPlan(tenantPlan);
        service.publishPlan(scoped.getId(), "filter-owner");
        service.createPlan(ignoredTenantPlan);

        PlanBoardQuery query = PlanBoardQuery.builder()
                .tenantId("tenant-board-filter")
                .customerIds(List.of("cust-filter-a"))
                .granularity(PlanBoardQuery.TimeGranularity.WEEK)
                .from(baseline.minusDays(1))
                .to(baseline.plusDays(7))
                .build();

        PlanBoardView board = service.getPlanBoard(query);

        assertThat(board.getCustomerGroups())
                .extracting(PlanBoardView.CustomerGroup::getCustomerId)
                .containsExactly("cust-filter-a");
        assertThat(board.getTimeBuckets())
                .allSatisfy(bucket -> assertThat(bucket.getPlans())
                        .extracting(PlanBoardView.PlanCard::getCustomerId)
                        .containsOnly("cust-filter-a"));
    }

    @Test
    @DisplayName("getPlanBoard returns empty structures when no plans match")
    void shouldReturnEmptyPlanBoardWhenNoPlans() {
        PlanBoardQuery query = PlanBoardQuery.builder()
                .tenantId("missing-tenant")
                .granularity(PlanBoardQuery.TimeGranularity.MONTH)
                .build();

        PlanBoardView board = service.getPlanBoard(query);

        assertThat(board.getMetrics().getTotalPlans()).isZero();
        assertThat(board.getCustomerGroups()).isEmpty();
        assertThat(board.getTimeBuckets()).isEmpty();
    }

    @Test
    void shouldRejectPlanCreationWhenTimeConflicts() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        CreatePlanCommand existing = new CreatePlanCommand(
                "tenant-conflict",
                "原计划",
                "原计划描述",
                "cust-conflict",
                "owner-conflict",
                start,
                start.plusHours(2),
                "Asia/Shanghai",
                List.of("owner-conflict"),
                List.of(new PlanNodeCommand(null, "节点A", "CHECKLIST", "owner-conflict", 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        Plan created = service.createPlan(existing);
        service.publishPlan(created.getId(), "admin");

        CreatePlanCommand conflicting = new CreatePlanCommand(
                "tenant-conflict",
                "冲突计划",
                "冲突描述",
                "cust-conflict",
                "owner-conflict",
                start.plusMinutes(30),
                start.plusHours(3),
                "Asia/Shanghai",
                List.of("owner-conflict"),
                List.of(new PlanNodeCommand(null, "节点B", "CHECKLIST", "owner-conflict", 1, 45,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );

        assertThatThrownBy(() -> service.createPlan(conflicting))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void shouldDetectConflictsForOwnerWindow() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        CreatePlanCommand existing = new CreatePlanCommand(
                "tenant-conflict",
                "计划一",
                "计划一描述",
                "cust-conflict",
                "owner-conflict",
                start,
                start.plusHours(2),
                "Asia/Shanghai",
                List.of("owner-conflict"),
                List.of(new PlanNodeCommand(null, "节点A", "CHECKLIST", "owner-conflict", 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        Plan created = service.createPlan(existing);
        service.publishPlan(created.getId(), "admin");

        List<Plan> conflicts = service.findConflictingPlans("tenant-conflict", null, "owner-conflict",
                start.plusMinutes(15), start.plusHours(1), null);

        assertThat(conflicts).isNotEmpty();
        assertThat(conflicts).extracting(Plan::getId).contains(created.getId());
    }

    @Test
    void shouldRejectPublishWhenConflictsDetected() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(3);
        CreatePlanCommand planA = new CreatePlanCommand(
                "tenant-conflict",
                "计划A",
                "计划A描述",
                "cust-conflict",
                "owner-conflict",
                start,
                start.plusHours(2),
                "Asia/Shanghai",
                List.of("owner-conflict"),
                List.of(new PlanNodeCommand(null, "节点A", "CHECKLIST", "owner-conflict", 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        CreatePlanCommand planB = new CreatePlanCommand(
                "tenant-conflict",
                "计划B",
                "计划B描述",
                "cust-conflict",
                "owner-conflict",
                start.plusMinutes(30),
                start.plusHours(3),
                "Asia/Shanghai",
                List.of("owner-conflict"),
                List.of(new PlanNodeCommand(null, "节点B", "CHECKLIST", "owner-conflict", 1, 45,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );

        Plan firstPlan = service.createPlan(planA);
        Plan secondPlan = service.createPlan(planB);

        service.publishPlan(firstPlan.getId(), "admin");

        assertThatThrownBy(() -> service.publishPlan(secondPlan.getId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void shouldCreatePlanWithExecutions() {
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-x",
                Localization.text(LocalizationKeys.Seeds.PLAN_PRIMARY_TITLE),
                Localization.text(LocalizationKeys.Seeds.PLAN_PRIMARY_DESCRIPTION),
                "cust-001",
                "admin",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                "Asia/Tokyo",
                List.of("admin"),
                List.of(new PlanNodeCommand(null,
                        Localization.text(LocalizationKeys.Seeds.PLAN_NODE_NOTIFY_TITLE),
                        "CHECKLIST", "admin", 1, 30, PlanNodeActionType.NONE, 100, null,
                        Localization.text(LocalizationKeys.Seeds.PLAN_NODE_NOTIFY_DESCRIPTION), List.of()))
        );

        var plan = service.createPlan(command);

        assertThat(plan.getExecutions()).hasSize(1);
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.DESIGN);
        assertThat(plan.getActivities())
                .extracting(activity -> activity.getType())
                .containsExactly(PlanActivityType.PLAN_CREATED);
    }

    @Test
    @DisplayName("listPlans filters by owner and keyword")
    void shouldFilterPlansByOwnerAndKeyword() {
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-y",
                "苏州数据中心巡检",
                "针对苏州机房的巡检计划",
                "cust-002",
                "ops-owner",
                OffsetDateTime.now().plusDays(2),
                OffsetDateTime.now().plusDays(2).plusHours(4),
                "Asia/Shanghai",
                List.of("ops-owner"),
                List.of(new PlanNodeCommand(null, "巡检准备", "CHECKLIST", "ops-owner", 1, 45, PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        service.createPlan(command);

        var result = service.listPlans(null, null, "ops-owner", "苏州", null, null, null, 0, 10);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.plans()).hasSize(1);
    }

    @Test
    @DisplayName("listPlans filters by tenant identifier")
    void shouldFilterPlansByTenant() {
        CreatePlanCommand tenantA = new CreatePlanCommand(
                "tenant-a",
                "东京日常巡检",
                "东京机房每周巡检",
                "cust-010",
                "owner-a",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                "Asia/Tokyo",
                List.of("owner-a"),
                List.of(new PlanNodeCommand(null, "检查UPS", "CHECKLIST", "owner-a", 1, 30, PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        CreatePlanCommand tenantB = new CreatePlanCommand(
                "tenant-b",
                "大阪应急演练",
                "演练跨区域灾备切换",
                "cust-020",
                "owner-b",
                OffsetDateTime.now().plusDays(2),
                OffsetDateTime.now().plusDays(2).plusHours(3),
                "Asia/Tokyo",
                List.of("owner-b"),
                List.of(new PlanNodeCommand(null, "切换预案讲解", "CHECKLIST", "owner-b", 1, 60, PlanNodeActionType.NONE, 100, null, "", List.of()))
        );

        service.createPlan(tenantA);
        service.createPlan(tenantB);

        var tenantAPlans = service.listPlans("tenant-a", null, null, null, null, null, null, 0, 10);
        assertThat(tenantAPlans.plans()).isNotEmpty();
        assertThat(tenantAPlans.plans()).allMatch(plan -> "tenant-a".equals(plan.getTenantId()));

        var tenantBPlans = service.listPlans("tenant-b", null, null, null, null, null, null, 0, 10);
        assertThat(tenantBPlans.plans()).isNotEmpty();
        assertThat(tenantBPlans.plans()).allMatch(plan -> "tenant-b".equals(plan.getTenantId()));
    }

    @Test
    @DisplayName("updateReminderPolicy replaces rules and appends timeline entry")
    void shouldUpdateReminderPolicy() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);
        List<PlanReminderRule> rules = List.of(
                new PlanReminderRule(null, PlanReminderTrigger.BEFORE_PLAN_START, 90,
                        List.of("EMAIL"), "template-90", List.of("OWNER"), "提前90分钟提醒负责人"));

        var updated = service.updateReminderPolicy(plan.getId(), rules, "admin");

        assertThat(updated.getReminderPolicy().getRules()).hasSize(1);
        assertThat(updated.getReminderPolicy().getRules().get(0).getOffsetMinutes()).isEqualTo(90);
        assertThat(updated.getReminderPolicy().getUpdatedBy()).isEqualTo("admin");
        assertThat(updated.getActivities())
                .extracting(activity -> activity.getType())
                .contains(PlanActivityType.REMINDER_POLICY_UPDATED);
    }

    @Test
    @DisplayName("previewReminderSchedule filters to future events")
    void shouldPreviewReminderSchedule() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);

        List<PlanReminderSchedule> schedule = service.previewReminderSchedule(plan.getId(), OffsetDateTime.now().minusDays(1));

        assertThat(schedule).isNotEmpty();
        assertThat(schedule)
                .allSatisfy(entry -> assertThat(entry.getFireTime()).isAfter(OffsetDateTime.now().minusDays(1)));
    }

    @Test
    @DisplayName("describeActivities exposes metadata for front-end dictionary")
    void shouldDescribeActivities() {
        var descriptors = service.describeActivities();

        assertThat(descriptors).isNotEmpty();
        assertThat(descriptors)
                .anySatisfy(descriptor -> {
                    if (descriptor.type() == PlanActivityType.PLAN_CREATED) {
                        assertThat(descriptor.messageKeys()).contains("plan.activity.created");
                        assertThat(descriptor.attributes())
                                .extracting(attr -> attr.name())
                                .contains("title", "owner");
                    }
                });
    }

    @Test
    void shouldStartNode() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);
        service.publishPlan(plan.getId(), "admin");
        PlanNodeExecution execution = service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin");

        assertThat(execution.getStatus()).isEqualTo(com.bob.mta.modules.plan.domain.PlanNodeStatus.IN_PROGRESS);
        assertThat(service.getPlan(plan.getId()).getStatus()).isIn(PlanStatus.IN_PROGRESS, PlanStatus.SCHEDULED);
    }

    @Test
    void shouldRenderIcs() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);
        service.cancelPlan(plan.getId(), "admin", "客户原因取消");
        String ics = service.renderPlanIcs(plan.getId());

        assertThat(ics).contains("BEGIN:VCALENDAR");
        assertThat(ics).contains("客户原因取消");
    }

    @Test
    @DisplayName("completeNode auto-completes parent and skips optional siblings when threshold satisfied")
    void shouldSkipOptionalSiblingsOnceThresholdReached() {
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-threshold",
                "阈值测试计划",
                "用于验证阈值自动完成逻辑",
                "cust-threshold",
                "admin",
                OffsetDateTime.now().plusHours(1),
                OffsetDateTime.now().plusHours(2),
                "Asia/Shanghai",
                List.of("admin"),
                List.of(new PlanNodeCommand(null, "父节点", "GROUP", "admin", 1, 30, PlanNodeActionType.NONE, 50, null, "",
                        List.of(
                                new PlanNodeCommand(null, "必做任务", "TASK", "admin", 1, 10, PlanNodeActionType.NONE, 100, null, "", List.of()),
                                new PlanNodeCommand(null, "可选任务", "TASK", "admin", 2, 10, PlanNodeActionType.NONE, 100, null, "", List.of())
                        )))
        );

        Plan created = service.createPlan(command);
        service.publishPlan(created.getId(), "admin");
        Plan reloaded = service.getPlan(created.getId());
        String parentId = reloaded.getNodes().get(0).getId();
        String requiredId = reloaded.getNodes().get(0).getChildren().get(0).getId();
        String optionalId = reloaded.getNodes().get(0).getChildren().get(1).getId();

        service.startNode(created.getId(), requiredId, "admin");
        service.completeNode(created.getId(), requiredId, "admin", "完成", null, List.of());

        Plan updated = service.getPlan(created.getId());
        Map<String, PlanNodeExecution> executionIndex = updated.getExecutions().stream()
                .collect(Collectors.toMap(PlanNodeExecution::getNodeId, Function.identity()));

        assertThat(executionIndex.get(parentId).getStatus()).isEqualTo(PlanNodeStatus.DONE);
        assertThat(executionIndex.get(optionalId).getStatus()).isEqualTo(PlanNodeStatus.SKIPPED);
        assertThat(updated.getStatus()).isEqualTo(PlanStatus.COMPLETED);
    }

    @Test
    void shouldThrowWhenMissing() {
        assertThatThrownBy(() -> service.getPlan("missing"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("cancelPlan stores reason and operator metadata")
    void shouldPersistCancellationMetadata() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);

        var updated = service.cancelPlan(plan.getId(), "operator", "客户要求顺延");

        assertThat(updated.getStatus()).isEqualTo(PlanStatus.CANCELED);
        assertThat(updated.getCancelReason()).isEqualTo("客户要求顺延");
        assertThat(updated.getCanceledBy()).isEqualTo("operator");
        assertThat(updated.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("handoverPlan updates owner and participants and appends timeline entry")
    void shouldHandoverPlan() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);

        var updated = service.handoverPlan(plan.getId(), "operator", List.of("operator", "observer"),
                "夜间交接", "admin");

        assertThat(updated.getOwner()).isEqualTo("operator");
        assertThat(updated.getParticipants()).containsExactlyInAnyOrder("operator", "observer");
        assertThat(updated.getActivities())
                .extracting(activity -> activity.getType())
                .contains(PlanActivityType.PLAN_HANDOVER);
        assertThat(updated.getActivities())
                .filteredOn(activity -> activity.getType() == PlanActivityType.PLAN_HANDOVER)
                .first()
                .extracting(activity -> activity.getAttributes().get("note"))
                .isEqualTo("夜间交接");
    }

    @Test
    @DisplayName("timeline captures node execution lifecycle")
    void shouldCaptureTimelineForNodeLifecycle() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);
        service.publishPlan(plan.getId(), "admin");
        String nodeId = plan.getExecutions().get(0).getNodeId();
        service.startNode(plan.getId(), nodeId, "operator");
        service.completeNode(plan.getId(), nodeId, "operator", "ok", null, null);

        var timeline = service.getPlanTimeline(plan.getId());

        assertThat(timeline)
                .extracting(entry -> entry.getType())
                .contains(PlanActivityType.PLAN_CREATED,
                        PlanActivityType.PLAN_PUBLISHED,
                        PlanActivityType.NODE_STARTED,
                        PlanActivityType.NODE_COMPLETED);
    }

    @Test
    @DisplayName("startNode rejects when plan is not published")
    void shouldRejectStartWhenDesign() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);

        assertThatThrownBy(() -> service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(messageResolver.getMessage("plan.error.planMustBePublished"));
    }

    @Test
    @DisplayName("startNode rejects when plan is canceled")
    void shouldRejectStartWhenCanceled() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);
        service.publishPlan(plan.getId(), "admin");
        service.cancelPlan(plan.getId(), "admin", "客户取消");

        assertThatThrownBy(() -> service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(messageResolver.getMessage("plan.error.planInactive"));
    }

    @Test
    @DisplayName("completeNode requires the node to be started first")
    void shouldRejectCompleteWhenPending() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);
        service.publishPlan(plan.getId(), "admin");

        assertThatThrownBy(() -> service.completeNode(plan.getId(), plan.getExecutions().get(0).getNodeId(),
                "admin", "ok", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(messageResolver.getMessage("plan.error.nodeMustBeStarted"));
    }

    @Test
    @DisplayName("completeNode rejects when plan is canceled mid-execution")
    void shouldRejectCompleteWhenPlanCanceled() {
        var plan = service.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0);
        service.publishPlan(plan.getId(), "admin");
        String nodeId = plan.getExecutions().get(0).getNodeId();
        service.startNode(plan.getId(), nodeId, "admin");
        service.cancelPlan(plan.getId(), "admin", "客户取消");

        assertThatThrownBy(() -> service.completeNode(plan.getId(), nodeId, "admin", "ok", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(messageResolver.getMessage("plan.error.planInactive"));
    }

    @Test
    @DisplayName("analytics aggregates plan states and upcoming queue")
    void shouldSummarizeAnalytics() {
        OffsetDateTime start = OffsetDateTime.now().minusHours(3);
        OffsetDateTime end = OffsetDateTime.now().minusHours(1);
        CreatePlanCommand overdueCommand = new CreatePlanCommand(
                "tenant-001",
                "应急排障",
                "计划在凌晨完成巡检",
                "cust-003",
                "admin",
                start,
                end,
                "Asia/Tokyo",
                List.of("admin"),
                List.of(new PlanNodeCommand(null, "巡检执行", "CHECKLIST", "admin", 1, 20, PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var overduePlan = service.createPlan(overdueCommand);
        service.publishPlan(overduePlan.getId(), "admin");

        var planToCancel = service.listPlans(null, null, null, null, null, null, null, 0, 20).plans().stream()
                .filter(plan -> !plan.getId().equals(overduePlan.getId()))
                .findFirst()
                .orElseThrow();
        service.cancelPlan(planToCancel.getId(), "admin", "客户取消");

        PlanAnalytics analytics = service.getAnalytics("tenant-001", null, null, null, null);

        assertThat(analytics.getTotalPlans()).isGreaterThanOrEqualTo(3);
        assertThat(analytics.getInProgressCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getCanceledCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getOverdueCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getUpcomingPlans()).isNotEmpty();
        assertThat(analytics.getOwnerLoads()).isNotEmpty();
        assertThat(analytics.getRiskPlans())
                .anySatisfy(risk -> assertThat(risk.getRiskLevel()).isEqualTo(PlanAnalytics.RiskLevel.OVERDUE));
    }

    @Test
    void analyticsShouldFilterByCustomer() {
        OffsetDateTime plannedStart = OffsetDateTime.now().plusDays(3);
        CreatePlanCommand targetCommand = new CreatePlanCommand(
                "tenant-analytics",
                "Network maintenance window",
                "Customer specific window",
                "cust-target",
                "owner-a",
                plannedStart,
                plannedStart.plusHours(2),
                "Asia/Tokyo",
                List.of("owner-a"),
                List.of(new PlanNodeCommand(null, "Validate routers", "CHECKLIST", "owner-a", 1, 30, PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var targetPlan = service.createPlan(targetCommand);
        service.publishPlan(targetPlan.getId(), "owner-a");

        CreatePlanCommand otherCommand = new CreatePlanCommand(
                "tenant-analytics",
                "Generic follow-up",
                "Second customer window",
                "cust-other",
                "owner-b",
                plannedStart.plusDays(1),
                plannedStart.plusDays(1).plusHours(1),
                "Asia/Tokyo",
                List.of("owner-b"),
                List.of(new PlanNodeCommand(null, "Confirm status", "CHECKLIST", "owner-b", 1, 15, PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        service.createPlan(otherCommand);

        PlanAnalytics analytics = service.getAnalytics("tenant-analytics", "cust-target", null, null, null);

        assertThat(analytics.getTotalPlans()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getUpcomingPlans())
                .allSatisfy(plan -> assertThat(plan.getCustomerId()).isEqualTo("cust-target"));
        assertThat(analytics.getDesignCount() + analytics.getScheduledCount() + analytics.getInProgressCount()
                + analytics.getCompletedCount() + analytics.getCanceledCount())
                .isEqualTo(analytics.getTotalPlans());
        assertThat(analytics.getOwnerLoads())
                .allSatisfy(load -> assertThat(load.getOwnerId()).isNotBlank());
    }

    @Test
    void analyticsShouldFilterByOwner() {
        OffsetDateTime plannedStart = OffsetDateTime.now().plusDays(2);
        CreatePlanCommand focusCommand = new CreatePlanCommand(
                "tenant-owner-scope",
                "Owner specific maintenance",
                "Target owner scope",
                "cust-scope",
                "owner-focus",
                plannedStart,
                plannedStart.plusHours(3),
                "Asia/Tokyo",
                List.of("owner-focus"),
                List.of(new PlanNodeCommand(null, "检查机柜", "CHECKLIST", "owner-focus", 1, 45,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var focusPlan = service.createPlan(focusCommand);
        service.publishPlan(focusPlan.getId(), "owner-focus");

        CreatePlanCommand otherOwner = new CreatePlanCommand(
                "tenant-owner-scope",
                "Other owner window",
                "Non target owner",
                "cust-scope",
                "owner-other",
                plannedStart.plusDays(1),
                plannedStart.plusDays(1).plusHours(2),
                "Asia/Tokyo",
                List.of("owner-other"),
                List.of(new PlanNodeCommand(null, "确认状态", "CHECKLIST", "owner-other", 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var otherPlan = service.createPlan(otherOwner);
        service.publishPlan(otherPlan.getId(), "owner-other");

        PlanAnalytics analytics = service.getAnalytics("tenant-owner-scope", null, "owner-focus", null, null);

        assertThat(analytics.getOwnerLoads()).hasSize(1);
        assertThat(analytics.getOwnerLoads().get(0).getOwnerId()).isEqualTo("owner-focus");
        assertThat(analytics.getUpcomingPlans())
                .allSatisfy(plan -> assertThat(plan.getOwner()).isEqualTo("owner-focus"));
        assertThat(analytics.getTotalPlans()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getRiskPlans())
                .allSatisfy(plan -> assertThat(plan.getOwner()).isEqualTo("owner-focus"));
    }

    @Test
    void shouldExecuteEmailActionWhenNodeStarts() {
        long templateId = 501L;
        templateService.register(templateId, new RenderedTemplate(
                "Node start", "Please start", List.of("ops@example.com"), List.of(), null,
                null, null, null, Map.of("context", "start")));

        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-action", "动作测试计划", "说明", "cust-action", "owner-action",
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(2),
                "Asia/Shanghai", List.of("owner-action"),
                List.of(new PlanNodeCommand(null, "执行检查", "CHECKLIST", "owner-action", 1, 30,
                        PlanNodeActionType.EMAIL, 100, String.valueOf(templateId), "", List.of()))
        );
        Plan created = service.createPlan(command);
        service.publishPlan(created.getId(), "owner-action");

        Plan updated = service.startNode(created.getId(), created.getNodes().get(0).getId(), "operator-x");

        assertThat(notificationGateway.getEmails()).hasSize(1);
        List<PlanActionHistory> histories = actionHistoryRepository.findByPlanId(created.getId());
        assertThat(histories).isNotEmpty();
        PlanActionHistory history = histories.get(histories.size() - 1);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.SUCCESS);
        assertThat(history.getMetadata()).containsEntry("templateId", String.valueOf(templateId));

        PlanActivity actionActivity = latestActivity(updated, PlanActivityType.NODE_ACTION_EXECUTED);
        assertThat(actionActivity).isNotNull();
        assertThat(actionActivity.getAttributes())
                .containsEntry("actionStatus", PlanActionStatus.SUCCESS.name())
                .containsEntry("meta.templateId", String.valueOf(templateId));
    }

    @Test
    void shouldRecordFailureWhenEmailGatewayFails() {
        long templateId = 777L;
        templateService.register(templateId, new RenderedTemplate(
                "Subject", "Body", List.of("ops@example.com"), List.of(), null,
                null, null, null, Map.of()));
        notificationGateway.failEmail("smtp-down");

        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-action-fail", "失败计划", "说明", "cust-action", "owner-action",
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(2),
                "Asia/Shanghai", List.of("owner-action"),
                List.of(new PlanNodeCommand(null, "执行检查", "CHECKLIST", "owner-action", 1, 30,
                        PlanNodeActionType.EMAIL, 100, String.valueOf(templateId), "", List.of()))
        );
        Plan created = service.createPlan(command);
        service.publishPlan(created.getId(), "owner-action");

        Plan updated = service.startNode(created.getId(), created.getNodes().get(0).getId(), "operator-y");

        List<PlanActionHistory> histories = actionHistoryRepository.findByPlanId(created.getId());
        assertThat(histories).isNotEmpty();
        PlanActionHistory history = histories.get(histories.size() - 1);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.FAILED);
        assertThat(history.getError()).contains("smtp-down");

        PlanActivity actionActivity = latestActivity(updated, PlanActivityType.NODE_ACTION_EXECUTED);
        assertThat(actionActivity.getAttributes())
                .containsEntry("actionStatus", PlanActionStatus.FAILED.name())
                .containsEntry("actionError", "smtp-down");
    }

    @Test
    void shouldSendInstantMessageWhenNodeCompletes() {
        long templateId = 888L;
        templateService.register(templateId, new RenderedTemplate(
                null, "完成提醒", List.of("duty-user"), List.of(), null,
                null, null, null, Map.of("channel", "IM")));

        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-action-im", "IM计划", "说明", "cust-action", "owner-action",
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(2),
                "Asia/Shanghai", List.of("owner-action"),
                List.of(new PlanNodeCommand(null, "执行检查", "CHECKLIST", "owner-action", 1, 30,
                        PlanNodeActionType.IM, 100, String.valueOf(templateId), "", List.of()))
        );
        Plan created = service.createPlan(command);
        service.publishPlan(created.getId(), "owner-action");
        service.startNode(created.getId(), created.getNodes().get(0).getId(), "operator-z");

        Plan completed = service.completeNode(created.getId(), created.getNodes().get(0).getId(),
                "operator-z", "OK", null, List.of());

        assertThat(notificationGateway.getInstantMessages()).hasSizeGreaterThanOrEqualTo(1);
        List<PlanActionHistory> imHistories = actionHistoryRepository.findByPlanId(created.getId());
        PlanActionHistory history = imHistories.get(imHistories.size() - 1);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.SUCCESS);
        assertThat(history.getMetadata()).containsEntry("templateId", String.valueOf(templateId));

        PlanActivity actionActivity = latestActivity(completed, PlanActivityType.NODE_ACTION_EXECUTED);
        assertThat(actionActivity.getAttributes())
                .containsEntry("actionStatus", PlanActionStatus.SUCCESS.name())
                .containsEntry("actionTrigger", "complete");
    }

    private PlanActivity latestActivity(Plan plan, PlanActivityType type) {
        return plan.getActivities().stream()
                .filter(activity -> activity.getType() == type)
                .reduce((first, second) -> second)
                .orElse(null);
    }
}
