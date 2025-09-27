package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.audit.domain.AuditLog;
import com.bob.mta.modules.audit.service.AuditQuery;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.dto.CancelPlanRequest;
import com.bob.mta.modules.plan.dto.CompleteNodeRequest;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.service.impl.InMemoryPlanService;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class PlanControllerTest {

    private PlanController controller;
    private InMemoryPlanService planService;
    private InMemoryAuditService auditService;

    @BeforeEach
    void setUp() {
        planService = new InMemoryPlanService(new InMemoryFileService());
        auditService = new InMemoryAuditService();
        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        controller = new PlanController(planService, recorder);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listShouldReturnPlans() {
        PageResponse<PlanSummaryResponse> page = controller.list(null, null, null, null, 0, 1).getData();
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void detailShouldReturnPlanWithNodes() {
        String planId = planService.listPlans(null, null, null, null).get(0).getId();
        PlanDetailResponse response = controller.detail(planId).getData();
        assertThat(response.getNodes()).isNotEmpty();
        assertThat(response.getTimeline()).isNotEmpty();
    }

    @Test
    void cancelShouldExposeReasonAndOperator() {
        String planId = planService.listPlans(null, null, null, null).get(0).getId();
        CancelPlanRequest request = new CancelPlanRequest();
        request.setReason("客户延期");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", null));

        PlanDetailResponse response = controller.cancel(planId, request).getData();

        assertThat(response.getCancelReason()).isEqualTo("客户延期");
        assertThat(response.getCanceledBy()).isEqualTo("admin");
        assertThat(response.getCanceledAt()).isNotNull();
    }

    @Test
    void publishShouldRecordBeforeAndAfterInAudit() {
        String planId = planService.listPlans(null, null, null, null).get(0).getId();
        authenticate("admin");

        controller.publish(planId);

        var logs = auditService.query(new AuditQuery("Plan", planId, "PUBLISH_PLAN", "admin"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldData()).contains("\"status\":\"DESIGN\"");
        assertThat(logs.get(0).getNewData()).contains("\"status\":");
    }

    @Test
    void timelineShouldExposePlanActivities() {
        String planId = planService.listPlans(null, null, null, null).get(0).getId();
        authenticate("admin");

        controller.publish(planId);
        var timeline = controller.timeline(planId).getData();

        assertThat(timeline)
                .extracting(entry -> entry.getType())
                .contains(PlanActivityType.PLAN_CREATED, PlanActivityType.PLAN_PUBLISHED);
    }

    @Test
    void startNodeShouldRecordStateTransitionInAudit() {
        String planId = planService.listPlans(null, null, null, null).get(0).getId();
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
        String planId = planService.listPlans(null, null, null, null).get(0).getId();
        String nodeId = planService.getPlan(planId).getExecutions().get(0).getNodeId();
        authenticate("operator");
        controller.publish(planId);
        controller.startNode(planId, nodeId);
        CompleteNodeRequest request = new CompleteNodeRequest();
        request.setResult("巡检完成");
        request.setLog("一切正常");

        controller.completeNode(planId, nodeId, request);

        var logs = auditService.query(new AuditQuery("PlanNode", planId + "::" + nodeId, "COMPLETE_NODE", "operator"));
        assertThat(logs).hasSize(1);
        AuditLog log = logs.get(0);
        assertThat(log.getOldData()).contains("\"status\":\"IN_PROGRESS\"");
        assertThat(log.getNewData()).contains("\"status\":\"DONE\"");
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(username, null));
    }
}
