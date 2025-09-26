package com.bob.mta.modules.plan.dto;

<<<<<<< HEAD
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;
import java.util.List;

public class PlanDetailResponse {

    private final String id;
    private final String title;
    private final String customerId;
    private final String owner;
    private final PlanStatus status;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final int progress;
    private final List<PlanNodeResponse> nodes;

    public PlanDetailResponse(String id, String title, String customerId, String owner, PlanStatus status,
                              OffsetDateTime startTime, OffsetDateTime endTime, int progress,
                              List<PlanNodeResponse> nodes) {
        this.id = id;
        this.title = title;
        this.customerId = customerId;
        this.owner = owner;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.progress = progress;
        this.nodes = nodes;
    }

    public static PlanDetailResponse from(Plan plan) {
        List<PlanNodeResponse> nodeResponses = plan.getNodes().stream()
                .map(PlanNodeResponse::from)
                .toList();
        return new PlanDetailResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getCustomerId(),
                plan.getOwner(),
                plan.getStatus(),
                plan.getStartTime(),
                plan.getEndTime(),
                plan.getProgress(),
                nodeResponses
        );
=======
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
>>>>>>> origin/main
    }

    public List<PlanNodeResponse> getNodes() {
        return nodes;
    }
}
