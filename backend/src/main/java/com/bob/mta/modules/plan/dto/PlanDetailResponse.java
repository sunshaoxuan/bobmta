package com.bob.mta.modules.plan.dto;

import java.time.Instant;
import java.util.List;

/**
 * Detail representation for design/execution view of plan.
 */
public class PlanDetailResponse {

    private final String id;

    private final String customerId;

    private final String title;

    private final String description;

    private final Instant startTime;

    private final Instant endTime;

    private final String status;

    private final List<String> assignees;

    private final List<PlanNodeResponse> nodes;

    public PlanDetailResponse(
            final String id,
            final String customerId,
            final String title,
            final String description,
            final Instant startTime,
            final Instant endTime,
            final String status,
            final List<String> assignees,
            final List<PlanNodeResponse> nodes) {
        this.id = id;
        this.customerId = customerId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.assignees = List.copyOf(assignees);
        this.nodes = List.copyOf(nodes);
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

    public String getDescription() {
        return description;
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

    public List<PlanNodeResponse> getNodes() {
        return nodes;
    }
}
