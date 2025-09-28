package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;

public record PlanEntity(
        String id,
        String tenantId,
        String customerId,
        String owner,
        String title,
        String description,
        PlanStatus status,
        OffsetDateTime plannedStartTime,
        OffsetDateTime plannedEndTime,
        OffsetDateTime actualStartTime,
        OffsetDateTime actualEndTime,
        String cancelReason,
        String canceledBy,
        OffsetDateTime canceledAt,
        String timezone,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime reminderUpdatedAt,
        String reminderUpdatedBy
) {
}
