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
    private final String cancelReason;
    private final String canceledBy;
    private final OffsetDateTime canceledAt;
    private final String timezone;
    private final int progress;
    private final List<PlanNode> nodes;
    private final List<PlanNodeExecution> executions;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final List<PlanActivity> activities;
    private final PlanReminderPolicy reminderPolicy;

    public Plan(String id, String tenantId, String title, String description, String customerId, String owner,
                List<String> participants, PlanStatus status, OffsetDateTime plannedStartTime,
                OffsetDateTime plannedEndTime, OffsetDateTime actualStartTime, OffsetDateTime actualEndTime,
                String cancelReason, String canceledBy, OffsetDateTime canceledAt, String timezone,
                List<PlanNode> nodes, List<PlanNodeExecution> executions,
                OffsetDateTime createdAt, OffsetDateTime updatedAt,
                List<PlanActivity> activities, PlanReminderPolicy reminderPolicy) {
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
        this.cancelReason = cancelReason;
        this.canceledBy = canceledBy;
        this.canceledAt = canceledAt;
        this.timezone = timezone;
        this.nodes = nodes == null ? List.of() : List.copyOf(nodes);
        this.executions = executions == null ? List.of() : List.copyOf(executions);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.activities = activities == null ? List.of() : List.copyOf(activities);
        this.reminderPolicy = reminderPolicy == null ? PlanReminderPolicy.empty() : reminderPolicy;
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

    public String getCancelReason() {
        return cancelReason;
    }

    public String getCanceledBy() {
        return canceledBy;
    }

    public OffsetDateTime getCanceledAt() {
        return canceledAt;
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
                           List<PlanNodeExecution> updatedExecutions, OffsetDateTime updatedAt,
                           String cancelReason, String canceledBy, OffsetDateTime canceledAt,
                           List<PlanActivity> newActivities) {
        return new Plan(id, tenantId, title, description, customerId, owner, participants, newStatus,
                plannedStartTime, plannedEndTime,
                actualStart != null ? actualStart : this.actualStartTime,
                actualEnd != null ? actualEnd : this.actualEndTime,
                cancelReason != null ? cancelReason : this.cancelReason,
                canceledBy != null ? canceledBy : this.canceledBy,
                canceledAt != null ? canceledAt : this.canceledAt,
                timezone, nodes, updatedExecutions, createdAt, updatedAt, newActivities, reminderPolicy);
    }

    public Plan withDefinition(List<PlanNode> newNodes, List<PlanNodeExecution> newExecutions,
                               OffsetDateTime updatedAt, OffsetDateTime newPlannedStart,
                               OffsetDateTime newPlannedEnd, String newDescription,
                               List<String> newParticipants, String newTimezone,
                               List<PlanActivity> newActivities) {
        return new Plan(id, tenantId, title, newDescription, customerId, owner, newParticipants, status,
                newPlannedStart, newPlannedEnd, actualStartTime, actualEndTime,
                cancelReason, canceledBy, canceledAt, newTimezone, newNodes, newExecutions, createdAt, updatedAt,
                newActivities, reminderPolicy);
    }

    public Plan withTitleAndOwner(String newTitle, String newOwner, OffsetDateTime updatedAt,
                                  List<PlanActivity> newActivities) {
        return new Plan(id, tenantId, newTitle, description, customerId, newOwner, participants, status,
                plannedStartTime, plannedEndTime, actualStartTime, actualEndTime,
                cancelReason, canceledBy, canceledAt, timezone, nodes, executions, createdAt, updatedAt,
                newActivities, reminderPolicy);
    }

    public Plan withReminderPolicy(PlanReminderPolicy newPolicy, OffsetDateTime updatedAt,
                                   List<PlanActivity> newActivities) {
        return new Plan(id, tenantId, title, description, customerId, owner, participants, status,
                plannedStartTime, plannedEndTime, actualStartTime, actualEndTime,
                cancelReason, canceledBy, canceledAt, timezone, nodes, executions, createdAt, updatedAt,
                newActivities, newPolicy);
    }

    public List<PlanActivity> getActivities() {
        return Collections.unmodifiableList(activities);
    }

    public PlanReminderPolicy getReminderPolicy() {
        return reminderPolicy;
    }
}
