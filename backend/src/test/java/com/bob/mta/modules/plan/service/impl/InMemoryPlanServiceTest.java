package com.bob.mta.modules.plan.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryPlanServiceTest {

    private final InMemoryPlanService service = new InMemoryPlanService();

    @Test
    @DisplayName("listPlans supports customerId filtering")
    void shouldFilterPlansByCustomer() {
        final PageResponse<PlanSummaryResponse> page = service.listPlans(1, 20, "201", null);

        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).customerId()).isEqualTo("201");
    }

    @Test
    @DisplayName("getPlan throws BusinessException when id missing")
    void shouldThrowWhenPlanMissing() {
        assertThatThrownBy(() -> service.getPlan("missing"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("plan detail exposes nodes and assignees")
    void shouldReturnPlanDetail() {
        final PlanDetailResponse detail = service.getPlan("PLAN-5001");

        assertThat(detail.getNodes()).isNotEmpty();
        assertThat(detail.getAssignees()).contains("admin");
    }
}

