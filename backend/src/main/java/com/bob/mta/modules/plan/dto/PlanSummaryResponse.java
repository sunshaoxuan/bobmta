package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;
import java.util.List;

public class PlanSummaryResponse {

    private final String id;
    private final String title;
    private final String customerId;
    private final String owner;
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
    private final List<String> participants;
    private final int reminderRuleCount;

    public PlanSummaryResponse(String id, String title, String customerId, String owner, PlanStatus status,
                               OffsetDateTime plannedStartTime, OffsetDateTime plannedEndTime,
                               OffsetDateTime actualStartTime, OffsetDateTime actualEndTime,
                               String cancelReason, String canceledBy, OffsetDateTime canceledAt,
                               String timezone, int progress, List<String> participants, int reminderRuleCount) {
        this.id = id;
        this.title = title;
        this.customerId = customerId;
        this.owner = owner;
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
        this.participants = participants;
        this.reminderRuleCount = reminderRuleCount;
    }

    public static PlanSummaryResponse from(Plan plan) {
        return new PlanSummaryResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getCustomerId(),
                plan.getOwner(),
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
                plan.getParticipants(),
                plan.getReminderPolicy().getRules().size()
        );
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getOwner() {
        return owner;
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

    public OffsetDateTime getStartTime() {
        return plannedStartTime;
    }

    public OffsetDateTime getEndTime() {
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

    public List<String> getParticipants() {
        return participants;
    }

    public int getReminderRuleCount() {
        return reminderRuleCount;
    }
}
