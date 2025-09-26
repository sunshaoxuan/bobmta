package com.bob.mta.modules.plan.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Node/step inside a maintenance plan.
 */
public class PlanNodeResponse {

    private final String id;

    private final String title;

    private final String type;

    private final String status;

    private final Duration estimatedDuration;

    private final Instant startedAt;

    private final Instant completedAt;

    private final List<String> assignees;

    public PlanNodeResponse(
            final String id,
            final String title,
            final String type,
            final String status,
            final Duration estimatedDuration,
            final Instant startedAt,
            final Instant completedAt,
            final List<String> assignees) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.status = status;
        this.estimatedDuration = estimatedDuration;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.assignees = List.copyOf(assignees);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public Duration getEstimatedDuration() {
        return estimatedDuration;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<String> getAssignees() {
        return assignees;
    }
}
