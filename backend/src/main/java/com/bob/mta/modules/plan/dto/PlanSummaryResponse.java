package com.bob.mta.modules.plan.dto;

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
}
