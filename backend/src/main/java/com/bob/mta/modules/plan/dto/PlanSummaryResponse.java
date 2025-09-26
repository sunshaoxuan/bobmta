package com.bob.mta.modules.plan.dto;

<<<<<<< HEAD
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;

public class PlanSummaryResponse {

    private final String id;
    private final String title;
    private final String customerId;
    private final String owner;
    private final PlanStatus status;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final int progress;

    public PlanSummaryResponse(String id, String title, String customerId, String owner, PlanStatus status,
                               OffsetDateTime startTime, OffsetDateTime endTime, int progress) {
        this.id = id;
        this.title = title;
        this.customerId = customerId;
        this.owner = owner;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.progress = progress;
    }

    public static PlanSummaryResponse from(Plan plan) {
        return new PlanSummaryResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getCustomerId(),
                plan.getOwner(),
                plan.getStatus(),
                plan.getStartTime(),
                plan.getEndTime(),
                plan.getProgress()
        );
=======
import java.time.Instant;
import java.util.List;

/**
 * Summary of maintenance plan for list/calendar views.
 */
public class PlanSummaryResponse {

    private final String id;

    private final String customerId;

    private final String title;

    private final Instant startTime;

    private final Instant endTime;

    private final String status;

    private final List<String> assignees;

    private final int totalNodes;

    private final int completedNodes;

    public PlanSummaryResponse(
            final String id,
            final String customerId,
            final String title,
            final Instant startTime,
            final Instant endTime,
            final String status,
            final List<String> assignees,
            final int totalNodes,
            final int completedNodes) {
        this.id = id;
        this.customerId = customerId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.assignees = List.copyOf(assignees);
        this.totalNodes = totalNodes;
        this.completedNodes = completedNodes;
>>>>>>> origin/main
    }

    public String getId() {
        return id;
    }

<<<<<<< HEAD
    public String getTitle() {
        return title;
    }

=======
>>>>>>> origin/main
    public String getCustomerId() {
        return customerId;
    }

<<<<<<< HEAD
    public String getOwner() {
        return owner;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public int getProgress() {
        return progress;
=======
    public String getTitle() {
        return title;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getAssignees() {
        return assignees;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public int getCompletedNodes() {
        return completedNodes;
>>>>>>> origin/main
    }
}
