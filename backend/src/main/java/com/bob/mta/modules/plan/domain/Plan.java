package com.bob.mta.modules.plan.domain;

import java.time.OffsetDateTime;
import java.util.List;

public class Plan {

    private final String id;
    private final String title;
    private final String customerId;
    private final String owner;
    private final PlanStatus status;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final int progress;
    private final List<PlanNode> nodes;

    public Plan(String id, String title, String customerId, String owner, PlanStatus status,
                OffsetDateTime startTime, OffsetDateTime endTime, int progress, List<PlanNode> nodes) {
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

    public List<PlanNode> getNodes() {
        return nodes;
    }
}
