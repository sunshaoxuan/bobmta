package com.bob.mta.modules.plan.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderSchedule;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.repository.InMemoryPlanRepository;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

class InMemoryPlanServiceTest {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();
    private final MessageResolver messageResolver = TestMessageResolverFactory.create();
    private final InMemoryPlanService service = new InMemoryPlanService(new InMemoryFileService(), repository,
            messageResolver);

    @BeforeEach
    void setUpLocale() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
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
                        "CHECKLIST", "admin", 1, 30, null,
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
                List.of(new PlanNodeCommand(null, "巡检准备", "CHECKLIST", "ops-owner", 1, 45, null, "", List.of()))
        );
        service.createPlan(command);

        List<Plan> filtered = service.listPlans(null, "ops-owner", "苏州", null, null, null);

        assertThat(filtered).hasSize(1);
    }

    @Test
    @DisplayName("updateReminderPolicy replaces rules and appends timeline entry")
    void shouldUpdateReminderPolicy() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
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
        var plan = service.listPlans(null, null, null, null, null, null).get(0);

        List<PlanReminderSchedule> schedule = service.previewReminderSchedule(plan.getId(), OffsetDateTime.now().minusDays(1));

        assertThat(schedule).isNotEmpty();
        assertThat(schedule)
                .allSatisfy(entry -> assertThat(entry.getFireTime()).isAfter(OffsetDateTime.now().minusDays(1)));
    }

    @Test
    void shouldStartNode() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");
        PlanNodeExecution execution = service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin");

        assertThat(execution.getStatus()).isEqualTo(com.bob.mta.modules.plan.domain.PlanNodeStatus.IN_PROGRESS);
        assertThat(service.getPlan(plan.getId()).getStatus()).isIn(PlanStatus.IN_PROGRESS, PlanStatus.SCHEDULED);
    }

    @Test
    void shouldRenderIcs() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
        service.cancelPlan(plan.getId(), "admin", "客户原因取消");
        String ics = service.renderPlanIcs(plan.getId());

        assertThat(ics).contains("BEGIN:VCALENDAR");
        assertThat(ics).contains("客户原因取消");
    }

    @Test
    void shouldThrowWhenMissing() {
        assertThatThrownBy(() -> service.getPlan("missing"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("cancelPlan stores reason and operator metadata")
    void shouldPersistCancellationMetadata() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);

        var updated = service.cancelPlan(plan.getId(), "operator", "客户要求顺延");

        assertThat(updated.getStatus()).isEqualTo(PlanStatus.CANCELED);
        assertThat(updated.getCancelReason()).isEqualTo("客户要求顺延");
        assertThat(updated.getCanceledBy()).isEqualTo("operator");
        assertThat(updated.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("handoverPlan updates owner and participants and appends timeline entry")
    void shouldHandoverPlan() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);

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
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
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
        var plan = service.listPlans(null, null, null, null, null, null).get(0);

        assertThatThrownBy(() -> service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(messageResolver.getMessage("plan.error.planMustBePublished"));
    }

    @Test
    @DisplayName("startNode rejects when plan is canceled")
    void shouldRejectStartWhenCanceled() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");
        service.cancelPlan(plan.getId(), "admin", "客户取消");

        assertThatThrownBy(() -> service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(messageResolver.getMessage("plan.error.planInactive"));
    }

    @Test
    @DisplayName("completeNode requires the node to be started first")
    void shouldRejectCompleteWhenPending() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");

        assertThatThrownBy(() -> service.completeNode(plan.getId(), plan.getExecutions().get(0).getNodeId(),
                "admin", "ok", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(messageResolver.getMessage("plan.error.nodeMustBeStarted"));
    }

    @Test
    @DisplayName("completeNode rejects when plan is canceled mid-execution")
    void shouldRejectCompleteWhenPlanCanceled() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
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
                List.of(new PlanNodeCommand(null, "巡检执行", "CHECKLIST", "admin", 1, 20, null, "", List.of()))
        );
        var overduePlan = service.createPlan(overdueCommand);
        service.publishPlan(overduePlan.getId(), "admin");

        var planToCancel = service.listPlans(null, null, null, null, null, null).stream()
                .filter(plan -> !plan.getId().equals(overduePlan.getId()))
                .findFirst()
                .orElseThrow();
        service.cancelPlan(planToCancel.getId(), "admin", "客户取消");

        PlanAnalytics analytics = service.getAnalytics("tenant-001", null, null);

        assertThat(analytics.getTotalPlans()).isGreaterThanOrEqualTo(3);
        assertThat(analytics.getInProgressCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getCanceledCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getOverdueCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getUpcomingPlans()).isNotEmpty();
    }
}
