package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.file.domain.FileMetadata;
import com.bob.mta.modules.file.service.FileService;
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.dto.CancelPlanRequest;
import com.bob.mta.modules.plan.dto.CompleteNodeRequest;
import com.bob.mta.modules.plan.dto.CreatePlanRequest;
import com.bob.mta.modules.plan.dto.PlanActivityResponse;
import com.bob.mta.modules.plan.dto.PlanAnalyticsResponse;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanNodeAttachmentResponse;
import com.bob.mta.modules.plan.dto.PlanNodeExecutionResponse;
import com.bob.mta.modules.plan.dto.PlanNodeRequest;
import com.bob.mta.modules.plan.dto.PlanHandoverRequest;
import com.bob.mta.modules.plan.dto.PlanReminderPolicyRequest;
import com.bob.mta.modules.plan.dto.PlanReminderPolicyResponse;
import com.bob.mta.modules.plan.dto.PlanReminderPreviewResponse;
import com.bob.mta.modules.plan.dto.PlanReminderRuleRequest;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.dto.UpdatePlanRequest;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
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
    private final FileService fileService;

    public PlanController(PlanService planService, AuditRecorder auditRecorder, FileService fileService) {
        this.planService = planService;
        this.auditRecorder = auditRecorder;
        this.fileService = fileService;
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
    @GetMapping("/analytics")
    public ApiResponse<PlanAnalyticsResponse> analytics(@RequestParam(required = false) String tenantId,
                                                        @RequestParam(required = false) OffsetDateTime from,
                                                        @RequestParam(required = false) OffsetDateTime to) {
        return ApiResponse.success(PlanAnalyticsResponse.from(planService.getAnalytics(tenantId, from, to)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/{id}")
    public ApiResponse<PlanDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.success(toDetailResponse(planService.getPlan(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/{id}/timeline")
    public ApiResponse<List<PlanActivityResponse>> timeline(@PathVariable String id) {
        List<PlanActivityResponse> timeline = planService.getPlanTimeline(id).stream()
                .map(PlanActivityResponse::from)
                .toList();
        return ApiResponse.success(timeline);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/{id}/reminders")
    public ApiResponse<PlanReminderPolicyResponse> reminderPolicy(@PathVariable String id) {
        Plan plan = planService.getPlan(id);
        return ApiResponse.success(PlanReminderPolicyResponse.from(plan.getReminderPolicy()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PutMapping("/{id}/reminders")
    public ApiResponse<PlanReminderPolicyResponse> updateReminderPolicy(@PathVariable String id,
                                                                        @Valid @RequestBody PlanReminderPolicyRequest request) {
        Plan before = planService.getPlan(id);
        Plan updated = planService.updateReminderPolicy(id, toReminderRules(request.getRules()), currentUsername());
        auditRecorder.record("Plan", id, "UPDATE_PLAN_REMINDERS",
                Localization.text(LocalizationKeys.Audit.PLAN_REMINDER_UPDATE),
                PlanReminderPolicyResponse.from(before.getReminderPolicy()),
                PlanReminderPolicyResponse.from(updated.getReminderPolicy()));
        return ApiResponse.success(PlanReminderPolicyResponse.from(updated.getReminderPolicy()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/{id}/reminders/preview")
    public ApiResponse<List<PlanReminderPreviewResponse>> previewReminders(@PathVariable String id,
                                                                           @RequestParam(required = false)
                                                                           OffsetDateTime referenceTime) {
        List<PlanReminderPreviewResponse> preview = planService.previewReminderSchedule(id, referenceTime).stream()
                .map(PlanReminderPreviewResponse::from)
                .toList();
        return ApiResponse.success(preview);
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
        PlanDetailResponse detail = toDetailResponse(plan);
        auditRecorder.record("Plan", plan.getId(), "CREATE_PLAN",
                Localization.text(LocalizationKeys.Audit.PLAN_CREATE), null, detail);
        return ApiResponse.success(detail);
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
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        Plan updated = planService.updatePlan(id, command);
        PlanDetailResponse afterSnapshot = toDetailResponse(updated);
        auditRecorder.record("Plan", id, "UPDATE_PLAN",
                Localization.text(LocalizationKeys.Audit.PLAN_UPDATE), beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        Plan before = planService.getPlan(id);
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        planService.deletePlan(id);
        auditRecorder.record("Plan", id, "DELETE_PLAN",
                Localization.text(LocalizationKeys.Audit.PLAN_DELETE), beforeSnapshot, null);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{id}/publish")
    public ApiResponse<PlanDetailResponse> publish(@PathVariable String id) {
        Plan before = planService.getPlan(id);
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        Plan updated = planService.publishPlan(id, currentUsername());
        PlanDetailResponse afterSnapshot = toDetailResponse(updated);
        auditRecorder.record("Plan", id, "PUBLISH_PLAN",
                Localization.text(LocalizationKeys.Audit.PLAN_PUBLISH), beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{id}/cancel")
    public ApiResponse<PlanDetailResponse> cancel(@PathVariable String id,
                                                  @RequestBody(required = false) CancelPlanRequest request) {
        String reason = request != null ? request.getReason() : null;
        Plan before = planService.getPlan(id);
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        Plan updated = planService.cancelPlan(id, currentUsername(), reason);
        PlanDetailResponse afterSnapshot = toDetailResponse(updated);
        auditRecorder.record("Plan", id, "CANCEL_PLAN",
                Localization.text(LocalizationKeys.Audit.PLAN_CANCEL), beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{id}/handover")
    public ApiResponse<PlanDetailResponse> handover(@PathVariable String id,
                                                    @Valid @RequestBody PlanHandoverRequest request) {
        Plan before = planService.getPlan(id);
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        Plan updated = planService.handoverPlan(id, request.getNewOwner(), request.getParticipants(),
                request.getNote(), currentUsername());
        PlanDetailResponse afterSnapshot = toDetailResponse(updated);
        auditRecorder.record("Plan", id, "HANDOVER_PLAN",
                Localization.text(LocalizationKeys.Audit.PLAN_HANDOVER), beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{planId}/nodes/{nodeId}/start")
    public ApiResponse<PlanNodeExecutionResponse> startNode(@PathVariable String planId,
                                                             @PathVariable String nodeId) {
        PlanNodeExecutionResponse before = snapshotExecution(planId, nodeId);
        PlanNodeExecution execution = planService.startNode(planId, nodeId, currentUsername());
        PlanNodeExecutionResponse after = PlanNodeExecutionResponse.from(execution, this::resolveAttachments);
        auditRecorder.record("PlanNode", planId + "::" + nodeId, "START_NODE",
                Localization.text(LocalizationKeys.Audit.PLAN_NODE_START), before, after);
        return ApiResponse.success(after);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{planId}/nodes/{nodeId}/complete")
    public ApiResponse<PlanNodeExecutionResponse> completeNode(@PathVariable String planId,
                                                                @PathVariable String nodeId,
                                                                @Valid @RequestBody CompleteNodeRequest request) {
        PlanNodeExecutionResponse before = snapshotExecution(planId, nodeId);
        PlanNodeExecution execution = planService.completeNode(planId, nodeId, currentUsername(),
                request.getResult(), request.getLog(), request.getFileIds());
        PlanNodeExecutionResponse after = PlanNodeExecutionResponse.from(execution, this::resolveAttachments);
        auditRecorder.record("PlanNode", planId + "::" + nodeId, "COMPLETE_NODE",
                Localization.text(LocalizationKeys.Audit.PLAN_NODE_COMPLETE), before, after);
        return ApiResponse.success(after);
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

    private List<PlanReminderRule> toReminderRules(List<PlanReminderRuleRequest> rules) {
        if (rules == null) {
            return List.of();
        }
        return rules.stream()
                .map(rule -> new PlanReminderRule(rule.getId(), rule.getTrigger(), rule.getOffsetMinutes(),
                        rule.getChannels(), rule.getTemplateId(), rule.getRecipients(), rule.getDescription()))
                .toList();
    }

    private PlanNodeExecutionResponse snapshotExecution(String planId, String nodeId) {
        Plan plan = planService.getPlan(planId);
        return plan.getExecutions().stream()
                .filter(exec -> exec.getNodeId().equals(nodeId))
                .findFirst()
                .map(execution -> PlanNodeExecutionResponse.from(execution, this::resolveAttachments))
                .orElseGet(() -> PlanNodeExecutionResponse.from(null, this::resolveAttachments));
    }

    private PlanDetailResponse toDetailResponse(Plan plan) {
        return PlanDetailResponse.from(plan, this::resolveAttachments);
    }

    private List<PlanNodeAttachmentResponse> resolveAttachments(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        return fileIds.stream()
                .map(fileService::get)
                .map(this::toAttachment)
                .toList();
    }

    private PlanNodeAttachmentResponse toAttachment(FileMetadata metadata) {
        String downloadUrl = fileService.buildDownloadUrl(metadata);
        return new PlanNodeAttachmentResponse(metadata.getId(), metadata.getFileName(), metadata.getContentType(),
                metadata.getSize(), downloadUrl);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "system";
        }
        return authentication.getName();
    }
}
