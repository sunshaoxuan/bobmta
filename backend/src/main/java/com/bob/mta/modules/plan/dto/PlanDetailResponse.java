package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanDetailResponse {

    private final String id;
    private final String tenantId;
    private final String title;
    private final String description;
    private final String customerId;
    private final String owner;
    private final List<String> participants;
    private final PlanStatus status;
    private final OffsetDateTime plannedStartTime;
    private final OffsetDateTime plannedEndTime;
    private final OffsetDateTime actualStartTime;
    private final OffsetDateTime actualEndTime;
    private final String cancelReason;
    private final String canceledBy;
    private final OffsetDateTime canceledAt;
    private final String timezone;
    private final int progress;
    private final List<PlanNodeResponse> nodes;

    public PlanDetailResponse(String id, String tenantId, String title, String description, String customerId,
                              String owner, List<String> participants, PlanStatus status,
                              OffsetDateTime plannedStartTime, OffsetDateTime plannedEndTime,
                              OffsetDateTime actualStartTime, OffsetDateTime actualEndTime,
                              String cancelReason, String canceledBy, OffsetDateTime canceledAt,
                              String timezone, int progress, List<PlanNodeResponse> nodes) {
        this.id = id;
        this.tenantId = tenantId;
        this.title = title;
        this.description = description;
        this.customerId = customerId;
        this.owner = owner;
        this.participants = participants;
        this.status = status;
        this.plannedStartTime = plannedStartTime;
        this.plannedEndTime = plannedEndTime;
        this.actualStartTime = actualStartTime;
        this.actualEndTime = actualEndTime;
        this.cancelReason = cancelReason;
        this.canceledBy = canceledBy;
        this.canceledAt = canceledAt;
        this.timezone = timezone;
        this.progress = progress;
        this.nodes = nodes;
    }

    public static PlanDetailResponse from(Plan plan) {
        Map<String, PlanNodeExecution> executionIndex = plan.getExecutions().stream()
                .collect(Collectors.toMap(PlanNodeExecution::getNodeId, execution -> execution));
        List<PlanNodeResponse> nodeResponses = plan.getNodes().stream()
                .map(node -> PlanNodeResponse.from(node, executionIndex))
                .toList();
        return new PlanDetailResponse(
                plan.getId(),
                plan.getTenantId(),
                plan.getTitle(),
                plan.getDescription(),
                plan.getCustomerId(),
                plan.getOwner(),
                plan.getParticipants(),
                plan.getStatus(),
                plan.getPlannedStartTime(),
                plan.getPlannedEndTime(),
                plan.getActualStartTime(),
                plan.getActualEndTime(),
                plan.getCancelReason(),
                plan.getCanceledBy(),
                plan.getCanceledAt(),
                plan.getTimezone(),
                plan.getProgress(),
                nodeResponses
        );
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getOwner() {
        return owner;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartTime() {
        return plannedStartTime;
    }

    public OffsetDateTime getEndTime() {
        return plannedEndTime;
    }

    public OffsetDateTime getPlannedStartTime() {
        return plannedStartTime;
    }

    public OffsetDateTime getPlannedEndTime() {
        return plannedEndTime;
    }

    public OffsetDateTime getActualStartTime() {
        return actualStartTime;
    }

    public OffsetDateTime getActualEndTime() {
        return actualEndTime;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public String getCanceledBy() {
        return canceledBy;
    }

    public OffsetDateTime getCanceledAt() {
        return canceledAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public int getProgress() {
        return progress;
    }

    public List<PlanNodeResponse> getNodes() {
        return nodes;
    }
}
