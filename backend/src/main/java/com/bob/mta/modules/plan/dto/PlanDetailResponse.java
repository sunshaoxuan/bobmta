package com.bob.mta.modules.plan.dto;

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

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public int getProgress() {
        return progress;
    }

    public List<PlanNodeResponse> getNodes() {
        return nodes;
    }
}
