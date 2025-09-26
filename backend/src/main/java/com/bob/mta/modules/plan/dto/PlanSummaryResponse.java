package com.bob.mta.modules.plan.dto;

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
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

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
    }
}
