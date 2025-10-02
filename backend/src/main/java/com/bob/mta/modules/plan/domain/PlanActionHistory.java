package com.bob.mta.modules.plan.domain;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class PlanActionHistory {

    private final String id;
    private final String planId;
    private final String nodeId;
    private final PlanNodeActionType actionType;
    private final String actionRef;
    private final OffsetDateTime triggeredAt;
    private final String triggeredBy;
    private final PlanActionStatus status;
    private final String message;
    private final String error;
    private final Map<String, String> context;
    private final Map<String, String> metadata;

    public PlanActionHistory(String id,
                             String planId,
                             String nodeId,
                             PlanNodeActionType actionType,
                             String actionRef,
                             OffsetDateTime triggeredAt,
                             String triggeredBy,
                             PlanActionStatus status,
                             String message,
                             String error,
                             Map<String, String> context,
                             Map<String, String> metadata) {
        this.id = Objects.requireNonNull(id, "id");
        this.planId = Objects.requireNonNull(planId, "planId");
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.actionRef = actionRef;
        this.triggeredAt = Objects.requireNonNull(triggeredAt, "triggeredAt");
        this.triggeredBy = triggeredBy;
        this.status = Objects.requireNonNull(status, "status");
        this.message = message;
        this.error = error;
        this.context = context == null ? Map.of() : Map.copyOf(context);
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public String getId() {
        return id;
    }

    public String getPlanId() {
        return planId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public PlanNodeActionType getActionType() {
        return actionType;
    }

    public String getActionRef() {
        return actionRef;
    }

    public OffsetDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public PlanActionStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public Map<String, String> getContext() {
        return Collections.unmodifiableMap(context);
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
}
