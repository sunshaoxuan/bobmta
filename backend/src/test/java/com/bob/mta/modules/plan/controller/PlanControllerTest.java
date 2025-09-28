package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.audit.domain.AuditLog;
import com.bob.mta.modules.audit.service.AuditQuery;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import com.bob.mta.modules.plan.dto.CancelPlanRequest;
import com.bob.mta.modules.plan.dto.CompleteNodeRequest;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanHandoverRequest;
import com.bob.mta.modules.plan.dto.PlanNodeAttachmentResponse;
import com.bob.mta.modules.plan.dto.PlanReminderPolicyRequest;
import com.bob.mta.modules.plan.dto.PlanReminderRuleRequest;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.repository.InMemoryPlanAnalyticsRepository;
import com.bob.mta.modules.plan.repository.InMemoryPlanRepository;
import com.bob.mta.modules.plan.service.impl.InMemoryPlanService;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PlanControllerTest {

    private PlanController controller;
    private InMemoryPlanService planService;
    private InMemoryPlanRepository planRepository;
    private InMemoryFileService fileService;
    private InMemoryAuditService auditService;
    private MessageResolver messageResolver;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        fileService = new InMemoryFileService();
        planRepository = new InMemoryPlanRepository();
        messageResolver = TestMessageResolverFactory.create();
        planService = new InMemoryPlanService(fileService, planRepository,
                new InMemoryPlanAnalyticsRepository(planRepository), messageResolver);
        auditService = new InMemoryAuditService();
        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        controller = new PlanController(planService, recorder, fileService, messageResolver);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void listShouldReturnPlans() {
        PageResponse<PlanSummaryResponse> page = controller.list(null, null, null, null, null, null, 0, 1).getData();
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void listShouldFilterByOwnerAndKeyword() {
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-002",
                "数据中心深度巡检",
                "针对苏州数据中心的全面巡检",
                "cust-003",
                "ops-lead",
                OffsetDateTime.now().plusDays(5),
                OffsetDateTime.now().plusDays(5).plusHours(3),
                "Asia/Shanghai",
                List.of("ops-lead"),
                List.of(new PlanNodeCommand(null, "巡检准备", "CHECKLIST", "ops-lead", 1, 60, null, "", List.of()))
        );
        planService.createPlan(command);

        PageResponse<PlanSummaryResponse> filtered = controller
                .list(null, "ops-lead", "数据中心", null, null, null, 0, 10)
                .getData();

        assertThat(filtered.getItems()).hasSize(1);
        assertThat(filtered.getItems().get(0).getOwner()).isEqualTo("ops-lead");
    }

    @Test
    void analyticsShouldSummarizePlans() {
        var analytics = controller.analytics(null, null, null, null).getData();

        assertThat(analytics.getTotalPlans()).isGreaterThanOrEqualTo(2);
        assertThat(analytics.getUpcomingPlans()).isNotEmpty();
    }

    @Test
    void analyticsShouldHighlightOverduePlans() {
        OffsetDateTime start = OffsetDateTime.now().minusHours(2);
        OffsetDateTime end = OffsetDateTime.now().minusHours(1);
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-001",
                "过期巡检",
                "客户临时巡检",
                "cust-001",
                "admin",
                start,
                end,
                "Asia/Tokyo",
                List.of("admin"),
                List.of(new PlanNodeCommand(null, "快速检查", "CHECKLIST", "admin", 1, 15, null, "", List.of()))
        );
        var created = planService.createPlan(command);
        planService.publishPlan(created.getId(), "admin");

        var analytics = controller.analytics(null, null, null, null).getData();

        assertThat(analytics.getOverdueCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void detailShouldReturnPlanWithNodesAndReminders() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        PlanDetailResponse response = controller.detail(planId).getData();
        assertThat(response.getNodes()).isNotEmpty();
        assertThat(response.getTimeline()).isNotEmpty();
        assertThat(response.getReminderPolicy().getRules()).isNotEmpty();
    }

    @Test
    void detailShouldExposeNodeAttachments() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        String nodeId = planService.getPlan(planId).getExecutions().get(0).getNodeId();
        authenticate("admin");
        controller.publish(planId);
        controller.startNode(planId, nodeId);
        var file = fileService.register("evidence.log", "text/plain", 128, "plan-files", "PLAN_NODE", nodeId,
                "admin");
        CompleteNodeRequest request = new CompleteNodeRequest();
        request.setResult("完成");
        request.setLog("上传巡检记录");
        request.setFileIds(List.of(file.getId()));

        controller.completeNode(planId, nodeId, request);

        PlanDetailResponse response = controller.detail(planId).getData();
        PlanNodeAttachmentResponse attachment = response.getNodes().get(0).getExecution().getAttachments().get(0);
        assertThat(attachment.getId()).isEqualTo(file.getId());
        assertThat(attachment.getDownloadUrl()).isEqualTo(fileService.buildDownloadUrl(file));
    }

    @Test
    void reminderPolicyShouldExposeDefaultRules() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();

        var policy = controller.reminderPolicy(planId).getData();

        assertThat(policy.getRules()).isNotEmpty();
        assertThat(policy.getRules().get(0).getId()).isNotBlank();
    }

    @Test
    void updateReminderPolicyShouldRecordAuditAndPersistRules() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        authenticate("admin");
        PlanReminderRuleRequest ruleRequest = new PlanReminderRuleRequest();
        ruleRequest.setTrigger(PlanReminderTrigger.BEFORE_PLAN_START);
        ruleRequest.setOffsetMinutes(45);
        ruleRequest.setChannels(List.of("EMAIL"));
        ruleRequest.setTemplateId("custom-template");
        ruleRequest.setRecipients(List.of("OWNER"));
        ruleRequest.setDescription("开始前45分钟提醒负责人");
        PlanReminderPolicyRequest request = new PlanReminderPolicyRequest();
        request.setRules(List.of(ruleRequest));

        var response = controller.updateReminderPolicy(planId, request).getData();

        assertThat(response.getRules()).hasSize(1);
        assertThat(response.getRules().get(0).getOffsetMinutes()).isEqualTo(45);

        var logs = auditService.query(new AuditQuery("Plan", planId, "UPDATE_PLAN_REMINDERS", "admin"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getNewData()).contains("custom-template");
    }

    @Test
    void previewRemindersShouldReturnUpcomingEntries() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();

        var preview = controller.previewReminders(planId, OffsetDateTime.now().minusMinutes(1)).getData();

        assertThat(preview).isNotEmpty();
        assertThat(preview).allSatisfy(entry -> assertThat(entry.getFireTime()).isAfter(OffsetDateTime.now().minusMinutes(1)));
    }

    @Test
    void cancelShouldExposeReasonAndOperator() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        CancelPlanRequest request = new CancelPlanRequest();
        request.setReason("客户延期");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", null));

        PlanDetailResponse response = controller.cancel(planId, request).getData();

        assertThat(response.getCancelReason()).isEqualTo("客户延期");
        assertThat(response.getCanceledBy()).isEqualTo("admin");
        assertThat(response.getCanceledAt()).isNotNull();
    }

    @Test
    void handoverShouldUpdateOwnerParticipantsAndAuditTrail() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        authenticate("admin");
        PlanHandoverRequest request = new PlanHandoverRequest();
        request.setNewOwner("operator");
        request.setParticipants(List.of("operator", "observer"));
        request.setNote("夜间值班交接");

        PlanDetailResponse response = controller.handover(planId, request).getData();

        assertThat(response.getOwner()).isEqualTo("operator");
        assertThat(response.getParticipants()).containsExactlyInAnyOrder("operator", "observer");
        var logs = auditService.query(new AuditQuery("Plan", planId, "HANDOVER_PLAN", "admin"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getNewData()).contains("operator");
        var timeline = controller.timeline(planId).getData();
        assertThat(timeline)
                .extracting(entry -> entry.getType())
                .contains(PlanActivityType.PLAN_HANDOVER);
    }

    @Test
    void publishShouldRecordBeforeAndAfterInAudit() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        authenticate("admin");

        controller.publish(planId);

        var logs = auditService.query(new AuditQuery("Plan", planId, "PUBLISH_PLAN", "admin"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldData()).contains("\"status\":\"DESIGN\"");
        assertThat(logs.get(0).getNewData()).contains("\"status\":");
    }

    @Test
    void timelineShouldExposePlanActivities() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        authenticate("admin");

        controller.publish(planId);
        var timeline = controller.timeline(planId).getData();

        assertThat(timeline)
                .extracting(entry -> entry.getType())
                .contains(PlanActivityType.PLAN_CREATED, PlanActivityType.PLAN_PUBLISHED);
    }

    @Test
    void startNodeShouldRecordStateTransitionInAudit() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        String nodeId = planService.getPlan(planId).getExecutions().get(0).getNodeId();
        authenticate("operator");
        controller.publish(planId);

        controller.startNode(planId, nodeId);

        var logs = auditService.query(new AuditQuery("PlanNode", planId + "::" + nodeId, "START_NODE", "operator"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldData()).contains("\"status\":\"PENDING\"");
        assertThat(logs.get(0).getNewData()).contains("\"status\":\"IN_PROGRESS\"");
    }

    @Test
    void completeNodeShouldRecordStateTransitionInAudit() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        String nodeId = planService.getPlan(planId).getExecutions().get(0).getNodeId();
        authenticate("operator");
        controller.publish(planId);
        controller.startNode(planId, nodeId);
        var file = fileService.register("result.txt", "text/plain", 256, "plan-files", "PLAN_NODE", nodeId,
                "operator");
        CompleteNodeRequest request = new CompleteNodeRequest();
        request.setResult("巡检完成");
        request.setLog("一切正常");
        request.setFileIds(List.of(file.getId()));

        var response = controller.completeNode(planId, nodeId, request).getData();

        var logs = auditService.query(new AuditQuery("PlanNode", planId + "::" + nodeId, "COMPLETE_NODE", "operator"));
        assertThat(logs).hasSize(1);
        AuditLog log = logs.get(0);
        assertThat(log.getOldData()).contains("\"status\":\"IN_PROGRESS\"");
        assertThat(log.getNewData()).contains("\"status\":\"DONE\"");
        assertThat(response.getAttachments())
                .extracting(PlanNodeAttachmentResponse::getId)
                .contains(file.getId());
        assertThat(response.getAttachments().get(0).getDownloadUrl())
                .isEqualTo(fileService.buildDownloadUrl(file));
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(username, null));
    }
}
