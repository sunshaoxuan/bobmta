package com.bob.mta.modules.plan.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

class InMemoryPlanServiceTest {

    private final InMemoryPlanService service = new InMemoryPlanService(new InMemoryFileService());

    @Test
    @DisplayName("createPlan initializes nodes and executions")
    void shouldCreatePlanWithExecutions() {
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-x",
                "测试计划",
                "巡检准备",
                "cust-001",
                "admin",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                "Asia/Tokyo",
                List.of("admin"),
                List.of(new PlanNodeCommand(null, "检查清单", "CHECKLIST", "admin", 1, 30, null, "", List.of()))
        );

        var plan = service.createPlan(command);

        assertThat(plan.getExecutions()).hasSize(1);
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.DESIGN);
    }

    @Test
    @DisplayName("startNode transitions plan to in-progress")
    void shouldStartNode() {
        var plan = service.listPlans(null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");
        PlanNodeExecution execution = service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin");

        assertThat(execution.getStatus()).isEqualTo(com.bob.mta.modules.plan.domain.PlanNodeStatus.IN_PROGRESS);
        assertThat(service.getPlan(plan.getId()).getStatus()).isIn(PlanStatus.IN_PROGRESS, PlanStatus.SCHEDULED);
    }

    @Test
    @DisplayName("renderPlanIcs produces calendar payload")
    void shouldRenderIcs() {
        var plan = service.listPlans(null, null, null, null).get(0);
        service.cancelPlan(plan.getId(), "admin", "客户原因取消");
        String ics = service.renderPlanIcs(plan.getId());

        assertThat(ics).contains("BEGIN:VCALENDAR");
        assertThat(ics).contains("客户原因取消");
    }

    @Test
    @DisplayName("getPlan throws for unknown id")
    void shouldThrowWhenMissing() {
        assertThatThrownBy(() -> service.getPlan("missing"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("cancelPlan stores reason and operator metadata")
    void shouldPersistCancellationMetadata() {
        var plan = service.listPlans(null, null, null, null).get(0);

        var updated = service.cancelPlan(plan.getId(), "operator", "客户要求顺延");

        assertThat(updated.getStatus()).isEqualTo(PlanStatus.CANCELED);
        assertThat(updated.getCancelReason()).isEqualTo("客户要求顺延");
        assertThat(updated.getCanceledBy()).isEqualTo("operator");
        assertThat(updated.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("startNode rejects when plan is not published")
    void shouldRejectStartWhenDesign() {
        var plan = service.listPlans(null, null, null, null).get(0);

        assertThatThrownBy(() -> service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("published");
    }

    @Test
    @DisplayName("startNode rejects when plan is canceled")
    void shouldRejectStartWhenCanceled() {
        var plan = service.listPlans(null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");
        service.cancelPlan(plan.getId(), "admin", "客户取消");

        assertThatThrownBy(() -> service.startNode(plan.getId(), plan.getExecutions().get(0).getNodeId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer active");
    }

    @Test
    @DisplayName("completeNode requires the node to be started first")
    void shouldRejectCompleteWhenPending() {
        var plan = service.listPlans(null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");

        assertThatThrownBy(() -> service.completeNode(plan.getId(), plan.getExecutions().get(0).getNodeId(),
                "admin", "ok", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("started");
    }

    @Test
    @DisplayName("completeNode rejects when plan is canceled mid-execution")
    void shouldRejectCompleteWhenPlanCanceled() {
        var plan = service.listPlans(null, null, null, null).get(0);
        service.publishPlan(plan.getId(), "admin");
        String nodeId = plan.getExecutions().get(0).getNodeId();
        service.startNode(plan.getId(), nodeId, "admin");
        service.cancelPlan(plan.getId(), "admin", "客户取消");

        assertThatThrownBy(() -> service.completeNode(plan.getId(), nodeId, "admin", "ok", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer active");
    }
}
