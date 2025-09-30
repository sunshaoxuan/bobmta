package com.bob.mta.modules.plan.persistence;

public record PlanOwnerLoadEntity(
        String ownerId,
        long totalPlans,
        long activePlans,
        long overduePlans
) {
}
