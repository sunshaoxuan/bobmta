package com.bob.mta.modules.plan.persistence;

import java.time.OffsetDateTime;

public record PlanBoardTimeBucketEntity(String bucketId,
                                        OffsetDateTime start,
                                        OffsetDateTime end,
                                        long totalPlans,
                                        long activePlans,
                                        long completedPlans,
                                        long overduePlans,
                                        long dueSoonPlans,
                                        long atRiskPlans) {
}

