package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.file.domain.FileMetadata;
import com.bob.mta.modules.file.service.FileService;
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.dto.CancelPlanRequest;
import com.bob.mta.modules.plan.dto.CompleteNodeRequest;
import com.bob.mta.modules.plan.dto.CreatePlanRequest;
import com.bob.mta.modules.plan.dto.PlanActionHistoryResponse;
import com.bob.mta.modules.plan.dto.PlanActivityResponse;
import com.bob.mta.modules.plan.dto.PlanActivityTypeMetadataResponse;
import com.bob.mta.modules.plan.dto.PlanAnalyticsResponse;
import com.bob.mta.modules.plan.dto.PlanBoardResponse;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanNodeAttachmentResponse;
import com.bob.mta.modules.plan.dto.PlanNodeHandoverRequest;
import com.bob.mta.modules.plan.dto.PlanNodeRequest;
import com.bob.mta.modules.plan.dto.PlanNodeStartRequest;
import com.bob.mta.modules.plan.dto.PlanHandoverRequest;
import com.bob.mta.modules.plan.dto.PlanFilterOptionsResponse;
import com.bob.mta.modules.plan.dto.PlanReminderPolicyRequest;
import com.bob.mta.modules.plan.dto.PlanReminderPolicyResponse;
import com.bob.mta.modules.plan.dto.PlanReminderPreviewResponse;
import com.bob.mta.modules.plan.dto.PlanReminderOptionsResponse;
import com.bob.mta.modules.plan.dto.PlanReminderRuleRequest;
import com.bob.mta.modules.plan.dto.PlanReminderUpdateRequest;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.dto.UpdatePlanRequest;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import com.bob.mta.modules.plan.service.command.UpdatePlanCommand;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;
import com.bob.mta.modules.plan.repository.PlanSearchCriteria;
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
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanService planService;
    private final AuditRecorder auditRecorder;
    private final FileService fileService;
    private final MessageResolver messageResolver;

    public PlanController(PlanService planService, AuditRecorder auditRecorder, FileService fileService,
                          MessageResolver messageResolver) {
        this.planService = planService;
        this.auditRecorder = auditRecorder;
        this.fileService = fileService;
        this.messageResolver = messageResolver;
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping
    public ApiResponse<PageResponse<PlanSummaryResponse>> list(@RequestParam(required = false) String tenantId,
                                                               @RequestParam(required = false) String customerId,
                                                               @RequestParam(required = false) String owner,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) PlanStatus status,
                                                               @RequestParam(required = false) OffsetDateTime from,
                                                               @RequestParam(required = false) OffsetDateTime to,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        var result = planService.listPlans(tenantId, customerId, owner, keyword, status, from, to, page, size);
        List<PlanSummaryResponse> pageItems = result.plans().stream()
                .map(PlanSummaryResponse::from)
                .toList();
        return ApiResponse.success(PageResponse.of(pageItems, result.totalCount(), page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/board")
    public ApiResponse<PlanBoardResponse> board(@RequestParam(required = false) String tenantId,
                                                @RequestParam(name = "customerId", required = false)
                                                List<String> customerIds,
                                                @RequestParam(required = false) String owner,
                                                @RequestParam(name = "status", required = false)
                                                List<PlanStatus> statuses,
                                                @RequestParam(required = false) OffsetDateTime from,
                                                @RequestParam(required = false) OffsetDateTime to,
                                                @RequestParam(defaultValue = "WEEK")
                                                PlanBoardGrouping granularity) {
        List<PlanStatus> sanitizedStatuses = sanitizeStatuses(statuses);
        List<String> sanitizedCustomers = sanitizeCustomerIds(customerIds);
        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .tenantId(StringUtils.hasText(tenantId) ? tenantId : null)
                .owner(StringUtils.hasText(owner) ? owner : null)
                .statuses(sanitizedStatuses)
                .customerIds(sanitizedCustomers)
                .from(from)
                .to(to)
                .build();
        PlanBoardGrouping grouping = granularity == null ? PlanBoardGrouping.WEEK : granularity;
        PlanBoardResponse response = PlanBoardResponse.from(planService.getPlanBoard(criteria, grouping));
        String tenantScope = StringUtils.hasText(criteria.getTenantId()) ? criteria.getTenantId() : "GLOBAL";
        auditRecorder.record("PlanBoard", tenantScope, "VIEW_PLAN_BOARD",
                messageResolver.getMessage(LocalizationKeys.Audit.PLAN_BOARD_VIEW), null, response);
        return ApiResponse.success(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/analytics")
    public ApiResponse<PlanAnalyticsResponse> analytics(@RequestParam(required = false) String tenantId,
                                                        @RequestParam(required = false) String customerId,
                                                        @RequestParam(required = false) String ownerId,
                                                        @RequestParam(required = false) OffsetDateTime from,
                                                        @RequestParam(required = false) OffsetDateTime to) {
        return ApiResponse.success(PlanAnalyticsResponse.from(
                planService.getAnalytics(tenantId, customerId, ownerId, from, to)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/filter-options")
    public ApiResponse<PlanFilterOptionsResponse> filterOptions(
            @RequestParam(required = false) String tenantId) {
        return ApiResponse.success(PlanFilterOptionsResponse.from(
                planService.describePlanFilters(tenantId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/activity-types")
    public ApiResponse<List<PlanActivityTypeMetadataResponse>> activityTypes() {
        List<PlanActivityTypeMetadataResponse> descriptors = planService.describeActivities().stream()
                .map(descriptor -> PlanActivityTypeMetadataResponse.from(descriptor, messageResolver))
                .toList();
        return ApiResponse.success(descriptors);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/reminder-options")
    public ApiResponse<PlanReminderOptionsResponse> reminderOptions() {
        return ApiResponse.success(PlanReminderOptionsResponse.from(
                planService.describeReminderOptions(), messageResolver));
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
    @GetMapping("/{id}/actions")
    public ApiResponse<List<PlanActionHistoryResponse>> actionHistory(@PathVariable String id) {
        List<PlanActionHistoryResponse> histories = planService.getPlanActionHistory(id).stream()
                .map(PlanActionHistoryResponse::from)
                .toList();
        auditRecorder.record("PlanAction", id, "VIEW_PLAN_ACTIONS",
                messageResolver.getMessage(LocalizationKeys.Audit.PLAN_ACTION_HISTORY_VIEW),
                null, histories);
        return ApiResponse.success(histories);
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
                messageResolver.getMessage("audit.plan.updateReminders"),
                PlanReminderPolicyResponse.from(before.getReminderPolicy()),
                PlanReminderPolicyResponse.from(updated.getReminderPolicy()));
        return ApiResponse.success(PlanReminderPolicyResponse.from(updated.getReminderPolicy()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PutMapping("/{planId}/reminders/{reminderId}")
    public ApiResponse<PlanDetailResponse> updateReminderRule(@PathVariable String planId,
                                                              @PathVariable String reminderId,
                                                              @Valid @RequestBody PlanReminderUpdateRequest request) {
        Plan before = planService.getPlan(planId);
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        String operator = resolveOperator(null);
        Plan updated = planService.updateReminderRule(planId, reminderId, request.getActive(),
                request.getOffsetMinutes(), operator);
        PlanDetailResponse afterSnapshot = toDetailResponse(updated);
        auditRecorder.record("PlanReminder", planId + "::" + reminderId, "UPDATE_REMINDER",
                messageResolver.getMessage("audit.plan.reminder.update"), beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
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
                messageResolver.getMessage("audit.plan.create"), null, detail);
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
                messageResolver.getMessage("audit.plan.update"), beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        Plan before = planService.getPlan(id);
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        planService.deletePlan(id);
        auditRecorder.record("Plan", id, "DELETE_PLAN",
                messageResolver.getMessage("audit.plan.delete"), beforeSnapshot, null);
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
                messageResolver.getMessage("audit.plan.publish"), beforeSnapshot, afterSnapshot);
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
                messageResolver.getMessage("audit.plan.cancel"), beforeSnapshot, afterSnapshot);
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
                messageResolver.getMessage("audit.plan.handover"), beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{planId}/nodes/{nodeId}/start")
    public ApiResponse<PlanDetailResponse> startNode(@PathVariable String planId,
                                                     @PathVariable String nodeId,
                                                     @Valid @RequestBody PlanNodeStartRequest request) {
        Plan before = planService.getPlan(planId);
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        String operator = resolveOperator(request.getOperatorId());
        Plan updated = planService.startNode(planId, nodeId, operator);
        PlanDetailResponse afterSnapshot = toDetailResponse(updated);
        auditRecorder.record("PlanNode", planId + "::" + nodeId, "START_NODE",
                messageResolver.getMessage("audit.plan.startNode"), beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{planId}/nodes/{nodeId}/complete")
    public ApiResponse<PlanDetailResponse> completeNode(@PathVariable String planId,
                                                        @PathVariable String nodeId,
                                                        @Valid @RequestBody CompleteNodeRequest request) {
        Plan before = planService.getPlan(planId);
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        String operator = resolveOperator(request.getOperatorId());
        Plan updated = planService.completeNode(planId, nodeId, operator,
                request.getResult(), request.getLog(), request.getFileIds());
        PlanDetailResponse afterSnapshot = toDetailResponse(updated);
        auditRecorder.record("PlanNode", planId + "::" + nodeId, "COMPLETE_NODE",
                messageResolver.getMessage("audit.plan.completeNode"),
                beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping("/{planId}/nodes/{nodeId}/handover")
    public ApiResponse<PlanDetailResponse> handoverNode(@PathVariable String planId,
                                                        @PathVariable String nodeId,
                                                        @Valid @RequestBody PlanNodeHandoverRequest request) {
        Plan before = planService.getPlan(planId);
        PlanDetailResponse beforeSnapshot = toDetailResponse(before);
        String operator = resolveOperator(request.getOperatorId());
        Plan updated = planService.handoverNode(planId, nodeId, request.getAssigneeId(), request.getComment(), operator);
        PlanDetailResponse afterSnapshot = toDetailResponse(updated);
        auditRecorder.record("PlanNode", planId + "::" + nodeId, "HANDOVER_NODE",
                messageResolver.getMessage("audit.plan.node.handover"),
                beforeSnapshot, afterSnapshot);
        return ApiResponse.success(afterSnapshot);
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

    private List<PlanStatus> sanitizeStatuses(List<PlanStatus> statuses) {
        if (statuses == null) {
            return null;
        }
        List<PlanStatus> sanitized = statuses.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return sanitized.isEmpty() ? null : sanitized;
    }

    private List<String> sanitizeCustomerIds(List<String> customerIds) {
        if (customerIds == null) {
            return null;
        }
        List<String> sanitized = customerIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        return sanitized.isEmpty() ? null : sanitized;
    }

    private List<PlanNodeCommand> toCommands(List<PlanNodeRequest> nodes) {
        return nodes.stream()
                .map(node -> new PlanNodeCommand(node.getId(), node.getName(), node.getType(), node.getAssignee(),
                        node.getOrder(), node.getExpectedDurationMinutes(), node.getActionType(),
                        node.getCompletionThreshold(), node.getActionRef(), node.getDescription(),
                        toCommands(node.getChildren())))
                .toList();
    }

    private List<PlanReminderRule> toReminderRules(List<PlanReminderRuleRequest> rules) {
        if (rules == null) {
            return List.of();
        }
        return rules.stream()
                .map(rule -> new PlanReminderRule(rule.getId(), rule.getTrigger(), rule.getOffsetMinutes(),
                        rule.getChannels(), rule.getTemplateId(), rule.getRecipients(), rule.getDescription(),
                        rule.getActive() == null || rule.getActive()))
                .toList();
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

    private String resolveOperator(String requestOperatorId) {
        if (requestOperatorId != null && !requestOperatorId.isBlank()) {
            return requestOperatorId;
        }
        return currentUsername();
    }
}
