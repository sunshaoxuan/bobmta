package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanActionHistory;
import com.bob.mta.modules.plan.domain.PlanActionStatus;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;

import java.time.OffsetDateTime;
import java.util.Map;

public class PlanActionHistoryResponse {

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

    public PlanActionHistoryResponse(String id,
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
        this.id = id;
        this.planId = planId;
        this.nodeId = nodeId;
        this.actionType = actionType;
        this.actionRef = actionRef;
        this.triggeredAt = triggeredAt;
        this.triggeredBy = triggeredBy;
        this.status = status;
        this.message = message;
        this.error = error;
        this.context = context;
        this.metadata = metadata;
    }

    public static PlanActionHistoryResponse from(PlanActionHistory history) {
        return new PlanActionHistoryResponse(
                history.getId(),
                history.getPlanId(),
                history.getNodeId(),
                history.getActionType(),
                history.getActionRef(),
                history.getTriggeredAt(),
                history.getTriggeredBy(),
                history.getStatus(),
                history.getMessage(),
                history.getError(),
                history.getContext(),
                history.getMetadata()
        );
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
        return context;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
