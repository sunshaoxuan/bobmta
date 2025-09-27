package com.bob.mta.modules.plan.domain;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class Plan {

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
    private final String timezone;
    private final int progress;
    private final List<PlanNode> nodes;
    private final List<PlanNodeExecution> executions;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public Plan(String id, String tenantId, String title, String description, String customerId, String owner,
                List<String> participants, PlanStatus status, OffsetDateTime plannedStartTime,
                OffsetDateTime plannedEndTime, OffsetDateTime actualStartTime, OffsetDateTime actualEndTime,
                String timezone, List<PlanNode> nodes, List<PlanNodeExecution> executions,
                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.title = title;
        this.description = description;
        this.customerId = customerId;
        this.owner = owner;
        this.participants = participants == null ? List.of() : List.copyOf(participants);
        this.status = status;
        this.plannedStartTime = plannedStartTime;
        this.plannedEndTime = plannedEndTime;
        this.actualStartTime = actualStartTime;
        this.actualEndTime = actualEndTime;
        this.timezone = timezone;
        this.nodes = nodes == null ? List.of() : List.copyOf(nodes);
        this.executions = executions == null ? List.of() : List.copyOf(executions);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.progress = calculateProgress(this.executions);
    }

    private int calculateProgress(List<PlanNodeExecution> executions) {
        if (executions.isEmpty()) {
            return 0;
        }
        long done = executions.stream()
                .filter(execution -> execution.getStatus() == PlanNodeStatus.DONE)
                .count();
        return (int) Math.round(done * 100.0 / executions.size());
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
        return Collections.unmodifiableList(participants);
    }

    public PlanStatus getStatus() {
        return status;
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

    public String getTimezone() {
        return timezone;
    }

    public int getProgress() {
        return progress;
    }

    public List<PlanNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<PlanNodeExecution> getExecutions() {
        return Collections.unmodifiableList(executions);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Plan withStatus(PlanStatus newStatus, OffsetDateTime actualStart, OffsetDateTime actualEnd,
                           List<PlanNodeExecution> updatedExecutions, OffsetDateTime updatedAt) {
        return new Plan(id, tenantId, title, description, customerId, owner, participants, newStatus,
                plannedStartTime, plannedEndTime,
                actualStart != null ? actualStart : this.actualStartTime,
                actualEnd != null ? actualEnd : this.actualEndTime,
                timezone, nodes, updatedExecutions, createdAt, updatedAt);
    }

    public Plan withDefinition(List<PlanNode> newNodes, List<PlanNodeExecution> newExecutions,
                               OffsetDateTime updatedAt, OffsetDateTime newPlannedStart,
                               OffsetDateTime newPlannedEnd, String newDescription,
                               List<String> newParticipants, String newTimezone) {
        return new Plan(id, tenantId, title, newDescription, customerId, owner, newParticipants, status,
                newPlannedStart, newPlannedEnd, actualStartTime, actualEndTime, newTimezone, newNodes,
                newExecutions, createdAt, updatedAt);
    }

    public Plan withTitleAndOwner(String newTitle, String newOwner, OffsetDateTime updatedAt) {
        return new Plan(id, tenantId, newTitle, description, customerId, newOwner, participants, status,
                plannedStartTime, plannedEndTime, actualStartTime, actualEndTime, timezone, nodes,
                executions, createdAt, updatedAt);
    }
}
