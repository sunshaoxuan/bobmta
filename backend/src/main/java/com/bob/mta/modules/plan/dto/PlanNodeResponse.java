package com.bob.mta.modules.plan.dto;

<<<<<<< HEAD
import com.bob.mta.modules.plan.domain.PlanNode;

import java.util.List;

public class PlanNodeResponse {

    private final String id;
    private final String name;
    private final String type;
    private final String assignee;
    private final int order;
    private final List<PlanNodeResponse> children;

    public PlanNodeResponse(String id, String name, String type, String assignee, int order,
                            List<PlanNodeResponse> children) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.assignee = assignee;
        this.order = order;
        this.children = children;
    }

    public static PlanNodeResponse from(PlanNode node) {
        List<PlanNodeResponse> childResponses = node.getChildren().stream()
                .map(PlanNodeResponse::from)
                .toList();
        return new PlanNodeResponse(node.getId(), node.getName(), node.getType(), node.getAssignee(), node.getOrder(),
                childResponses);
=======
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
>>>>>>> origin/main
    }

    public String getId() {
        return id;
    }

<<<<<<< HEAD
    public String getName() {
        return name;
=======
    public String getTitle() {
        return title;
>>>>>>> origin/main
    }

    public String getType() {
        return type;
    }

<<<<<<< HEAD
    public String getAssignee() {
        return assignee;
    }

    public int getOrder() {
        return order;
    }

    public List<PlanNodeResponse> getChildren() {
        return children;
=======
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
>>>>>>> origin/main
    }
}
