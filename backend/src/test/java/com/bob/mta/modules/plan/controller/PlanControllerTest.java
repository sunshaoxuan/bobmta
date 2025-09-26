package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.service.impl.InMemoryPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanControllerTest {

    private PlanController controller;

    @BeforeEach
    void setUp() {
        controller = new PlanController(new InMemoryPlanService());
    }

    @Test
    void listShouldReturnPlans() {
        PageResponse<PlanSummaryResponse> page = controller.list("", "", 0, 1).getData();
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void detailShouldReturnPlanWithNodes() {
        PlanDetailResponse response = controller.detail("plan-001").getData();
        assertThat(response.getNodes()).isNotEmpty();
    }
}
