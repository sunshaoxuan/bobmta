package com.bob.mta.modules.plan.persistence;

import java.time.OffsetDateTime;

public record PlanBoardCustomerAggregateEntity(String customerId,
                                               String customerName,
                                               long totalPlans,
                                               long activePlans,
                                               long completedPlans,
                                               long overduePlans,
                                               long dueSoonPlans,
                                               double averageProgress,
                                               OffsetDateTime earliestStart,
                                               OffsetDateTime latestEnd) {
}

