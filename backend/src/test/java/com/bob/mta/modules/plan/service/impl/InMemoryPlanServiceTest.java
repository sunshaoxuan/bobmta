package com.bob.mta.modules.plan.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
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
    void shouldUpdateReminderPolicy() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
        List<PlanReminderRule> rules = List.of(
                new PlanReminderRule(null, PlanReminderTrigger.BEFORE_PLAN_START, 90,
                        List.of("EMAIL"), "template-90", List.of("OWNER"),
                        Localization.text(LocalizationKeys.Seeds.PLAN_REMINDER_SECOND)));

        var updated = service.updateReminderPolicy(plan.getId(), rules, "admin");

        assertThat(updated.getReminderPolicy().getRules()).hasSize(1);
        assertThat(updated.getReminderPolicy().getRules().get(0).getOffsetMinutes()).isEqualTo(90);
        assertThat(updated.getReminderPolicy().getUpdatedBy()).isEqualTo("admin");
        assertThat(updated.getActivities())
                .extracting(activity -> activity.getType())
                .contains(PlanActivityType.REMINDER_POLICY_UPDATED);
    }

    @Test
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
        var plan = service.listPlans(null, null, null, null).get(0);
        service.cancelPlan(plan.getId(), "admin",
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_CANCELLED));
        String ics = service.renderPlanIcs(plan.getId());

        assertThat(ics).contains("BEGIN:VCALENDAR");
        assertThat(ics).contains(Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_CANCELLED));
    }

    @Test
    void shouldThrowWhenMissing() {
        assertThatThrownBy(() -> service.getPlan("missing"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldPersistCancellationMetadata() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);

        var updated = service.cancelPlan(plan.getId(), "operator", "R-001");

        assertThat(updated.getStatus()).isEqualTo(PlanStatus.CANCELED);
        assertThat(updated.getCancelReason()).isEqualTo("R-001");
        assertThat(updated.getCanceledBy()).isEqualTo("operator");
        assertThat(updated.getCanceledAt()).isNotNull();
    }

    @Test
    void shouldHandoverPlan() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);

        var updated = service.handoverPlan(plan.getId(), "operator", List.of("operator", "observer"),
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_HANDOVER), "admin");

        assertThat(updated.getOwner()).isEqualTo("operator");
        assertThat(updated.getParticipants()).containsExactlyInAnyOrder("operator", "observer");
        assertThat(updated.getActivities())
                .extracting(activity -> activity.getType())
                .contains(PlanActivityType.PLAN_HANDOVER);
        assertThat(updated.getActivities())
                .filteredOn(activity -> activity.getType() == PlanActivityType.PLAN_HANDOVER)
                .first()
                .extracting(activity -> activity.getAttributes().get("note"))
                .isEqualTo(Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_HANDOVER));
    }

    @Test
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
    void shouldRejectStartWhenDesign() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);

        assertThatThrownBy(() -> service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(Localization.text(LocalizationKeys.Errors.PLAN_EXECUTE_REQUIRES_PUBLISH));
    }

    @Test
    void shouldRejectStartWhenCanceled() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");
        service.cancelPlan(plan.getId(), "admin",
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_CANCELLED));

        assertThatThrownBy(() -> service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(Localization.text(LocalizationKeys.Errors.PLAN_INACTIVE));
    }

    @Test
    void shouldRejectCompleteWhenPending() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");

        assertThatThrownBy(() -> service.completeNode(plan.getId(), plan.getExecutions().get(0).getNodeId(),
                "admin", "ok", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(Localization.text(LocalizationKeys.Errors.PLAN_NODE_REQUIRES_START));
    }

    @Test
    void shouldRejectCompleteWhenPlanCanceled() {
        var plan = service.listPlans(null, null, null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");
        String nodeId = plan.getExecutions().get(0).getNodeId();
        service.startNode(plan.getId(), nodeId, "admin");
        service.cancelPlan(plan.getId(), "admin",
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_CANCELLED));

        assertThatThrownBy(() -> service.completeNode(plan.getId(), nodeId, "admin", "ok", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(Localization.text(LocalizationKeys.Errors.PLAN_INACTIVE));
    }

    @Test
    void shouldSummarizeAnalytics() {
        OffsetDateTime start = OffsetDateTime.now().minusHours(3);
        OffsetDateTime end = OffsetDateTime.now().minusHours(1);
        CreatePlanCommand overdueCommand = new CreatePlanCommand(
                "tenant-001",
                Localization.text(LocalizationKeys.Seeds.PLAN_PRIMARY_TITLE),
                Localization.text(LocalizationKeys.Seeds.PLAN_PRIMARY_DESCRIPTION),
                "cust-003",
                "admin",
                start,
                end,
                "Asia/Tokyo",
                List.of("admin"),
                List.of(new PlanNodeCommand(null,
                        Localization.text(LocalizationKeys.Seeds.PLAN_NODE_BACKUP_TITLE),
                        "CHECKLIST", "admin", 1, 20, null,
                        Localization.text(LocalizationKeys.Seeds.PLAN_NODE_BACKUP_DESCRIPTION), List.of()))
        );
        var overduePlan = service.createPlan(overdueCommand);
        service.publishPlan(overduePlan.getId(), "admin");

        var planToCancel = service.listPlans(null, null, null, null, null, null).stream()
                .filter(plan -> !plan.getId().equals(overduePlan.getId()))
                .findFirst()
                .orElseThrow();
        service.cancelPlan(planToCancel.getId(), "admin",
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_CANCELLED));

        PlanAnalytics analytics = service.getAnalytics("tenant-001", null, null);

        assertThat(analytics.getTotalPlans()).isGreaterThanOrEqualTo(3);
        assertThat(analytics.getInProgressCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getCanceledCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getOverdueCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getUpcomingPlans()).isNotEmpty();
    }
}
