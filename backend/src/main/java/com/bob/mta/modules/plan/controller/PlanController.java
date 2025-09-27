package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.dto.CancelPlanRequest;
import com.bob.mta.modules.plan.dto.CompleteNodeRequest;
import com.bob.mta.modules.plan.dto.CreatePlanRequest;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanNodeExecutionResponse;
import com.bob.mta.modules.plan.dto.PlanNodeRequest;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.dto.UpdatePlanRequest;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import com.bob.mta.modules.plan.service.command.UpdatePlanCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanService planService;
    private final AuditRecorder auditRecorder;

    public PlanController(PlanService planService, AuditRecorder auditRecorder) {
        this.planService = planService;
        this.auditRecorder = auditRecorder;
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping
    public ApiResponse<PageResponse<PlanSummaryResponse>> list(@RequestParam(required = false) String customerId,
                                                               @RequestParam(required = false) PlanStatus status,
                                                               @RequestParam(required = false) OffsetDateTime from,
                                                               @RequestParam(required = false) OffsetDateTime to,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        List<Plan> plans = planService.listPlans(customerId, status, from, to);
        List<PlanSummaryResponse> summaries = plans.stream().map(PlanSummaryResponse::from).toList();
        int fromIndex = Math.min(page * size, summaries.size());
        int toIndex = Math.min(fromIndex + size, summaries.size());
        List<PlanSummaryResponse> pageItems = summaries.subList(fromIndex, toIndex);
        return ApiResponse.success(PageResponse.of(pageItems, summaries.size(), page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/{id}")
    public ApiResponse<PlanDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.success(PlanDetailResponse.from(planService.getPlan(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping
    public ApiResponse<PlanDetailResponse> create(@Valid @RequestBody CreatePlanRequest request) {
        CreatePlanCommand command = new CreatePlanCommand(
                request.getTenantId(),
                request.getTitle(),
                request.getDescription(),
                request.getCustomerId(),
                request.getOwner(),
                request.getStartTime(),
                request.getEndTime(),
                request.getTimezone(),
                request.getParticipants(),
                toCommands(request.getNodes())
        );
        Plan plan = planService.createPlan(command);
        auditRecorder.record("Plan", plan.getId(), "CREATE_PLAN", "创建计划", null, PlanDetailResponse.from(plan));
        return ApiResponse.success(PlanDetailResponse.from(plan));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PutMapping("/{id}")
    public ApiResponse<PlanDetailResponse> update(@PathVariable String id, @Valid @RequestBody UpdatePlanRequest request) {
        UpdatePlanCommand command = new UpdatePlanCommand(
                request.getTitle(),
                request.getDescription(),
                request.getStartTime(),
                request.getEndTime(),
                request.getTimezone(),
                request.getParticipants(),
                toCommands(request.getNodes())
        );
        Plan before = planService.getPlan(id);
        Plan updated = planService.updatePlan(id, command);
        auditRecorder.record("Plan", id, "UPDATE_PLAN", "更新计划", PlanDetailResponse.from(before),
                PlanDetailResponse.from(updated));
        return ApiResponse.success(PlanDetailResponse.from(updated));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        Plan before = planService.getPlan(id);
        planService.deletePlan(id);
        auditRecorder.record("Plan", id, "DELETE_PLAN", "删除计划", PlanDetailResponse.from(before), null);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{id}/publish")
    public ApiResponse<PlanDetailResponse> publish(@PathVariable String id) {
        Plan updated = planService.publishPlan(id, currentUsername());
        auditRecorder.record("Plan", id, "PUBLISH_PLAN", "发布计划", null, PlanDetailResponse.from(updated));
        return ApiResponse.success(PlanDetailResponse.from(updated));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{id}/cancel")
    public ApiResponse<PlanDetailResponse> cancel(@PathVariable String id,
                                                  @RequestBody(required = false) CancelPlanRequest request) {
        String reason = request != null ? request.getReason() : null;
        Plan updated = planService.cancelPlan(id, currentUsername(), reason);
        auditRecorder.record("Plan", id, "CANCEL_PLAN", "取消计划", null, PlanDetailResponse.from(updated));
        return ApiResponse.success(PlanDetailResponse.from(updated));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{planId}/nodes/{nodeId}/start")
    public ApiResponse<PlanNodeExecutionResponse> startNode(@PathVariable String planId,
                                                             @PathVariable String nodeId) {
        PlanNodeExecution execution = planService.startNode(planId, nodeId, currentUsername());
        auditRecorder.record("PlanNode", planId + "::" + nodeId, "START_NODE", "开始执行节点", null,
                PlanNodeExecutionResponse.from(execution));
        return ApiResponse.success(PlanNodeExecutionResponse.from(execution));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{planId}/nodes/{nodeId}/complete")
    public ApiResponse<PlanNodeExecutionResponse> completeNode(@PathVariable String planId,
                                                                @PathVariable String nodeId,
                                                                @Valid @RequestBody CompleteNodeRequest request) {
        PlanNodeExecution execution = planService.completeNode(planId, nodeId, currentUsername(),
                request.getResult(), request.getLog(), request.getFileIds());
        auditRecorder.record("PlanNode", planId + "::" + nodeId, "COMPLETE_NODE", "完成节点",
                null, PlanNodeExecutionResponse.from(execution));
        return ApiResponse.success(PlanNodeExecutionResponse.from(execution));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping(value = "/{id}/ics", produces = "text/calendar")
    public ResponseEntity<String> downloadIcs(@PathVariable String id) {
        String content = planService.renderPlanIcs(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + id + ".ics")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(content);
    }

    private List<PlanNodeCommand> toCommands(List<PlanNodeRequest> nodes) {
        return nodes.stream()
                .map(node -> new PlanNodeCommand(node.getId(), node.getName(), node.getType(), node.getAssignee(),
                        node.getOrder(), node.getExpectedDurationMinutes(), node.getActionRef(), node.getDescription(),
                        toCommands(node.getChildren())))
                .toList();
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        }
        return authentication.getName();
    }
}
