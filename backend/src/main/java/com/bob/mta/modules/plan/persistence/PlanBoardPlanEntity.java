package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;

public record PlanBoardPlanEntity(String planId,
                                  String title,
                                  PlanStatus status,
                                  String ownerId,
                                  String customerId,
                                  OffsetDateTime plannedStartTime,
                                  OffsetDateTime plannedEndTime,
                                  String timezone,
                                  Integer progress,
                                  boolean overdue,
                                  boolean dueSoon,
                                  Long minutesUntilDue,
                                  Long minutesOverdue) {
}

