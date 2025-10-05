package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanActionStatus;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;

import java.time.OffsetDateTime;
import java.util.Map;

public record PlanActionHistoryEntity(
        String actionId,
        String planId,
        String nodeId,
        PlanNodeActionType actionType,
        String actionRef,
        OffsetDateTime triggeredAt,
        String triggeredBy,
        PlanActionStatus status,
        String messageKey,
        String errorMessage,
        Map<String, String> context,
        Map<String, String> metadata) {
}
