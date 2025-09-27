package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.plan.dto.CancelPlanRequest;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.service.impl.InMemoryPlanService;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class PlanControllerTest {

    private PlanController controller;
    private InMemoryPlanService planService;

    @BeforeEach
    void setUp() {
        planService = new InMemoryPlanService(new InMemoryFileService());
        AuditRecorder recorder = new AuditRecorder(new InMemoryAuditService(), new ObjectMapper());
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
}
