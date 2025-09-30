package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;

public record PlanRiskPlanEntity(
        String planId,
        String title,
        PlanStatus status,
        OffsetDateTime plannedEndTime,
        String ownerId,
        String customerId,
        String riskLevel,
        Long minutesUntilDue,
        Long minutesOverdue
) {
}
