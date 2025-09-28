package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanNodeStatus;

import java.time.OffsetDateTime;

public record PlanNodeExecutionEntity(
        String planId,
        String nodeId,
        PlanNodeStatus status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String operator,
        String result,
        String log
) {
}
