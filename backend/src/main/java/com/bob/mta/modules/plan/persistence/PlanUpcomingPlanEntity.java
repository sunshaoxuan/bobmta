package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;

public record PlanUpcomingPlanEntity(
        String planId,
        String title,
        PlanStatus status,
        OffsetDateTime plannedStartTime,
        OffsetDateTime plannedEndTime,
        String ownerId,
        String customerId,
        Long completedNodes,
        Long totalNodes
) {
}
