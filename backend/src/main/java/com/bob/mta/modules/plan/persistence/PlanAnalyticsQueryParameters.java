package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.repository.PlanAnalyticsQuery;

import java.time.OffsetDateTime;

public record PlanAnalyticsQueryParameters(
        String tenantId,
        String customerId,
        OffsetDateTime plannedStartFrom,
        OffsetDateTime plannedEndTo,
        OffsetDateTime referenceTime,
        Integer upcomingLimit
) {

    public static PlanAnalyticsQueryParameters fromQuery(PlanAnalyticsQuery query) {
        if (query == null) {
            return new PlanAnalyticsQueryParameters(null, null, null, null, OffsetDateTime.now(), 5);
        }
        return new PlanAnalyticsQueryParameters(
                query.getTenantId(),
                query.getCustomerId(),
                query.getFrom(),
                query.getTo(),
                query.getReferenceTime(),
                query.getUpcomingLimit()
        );
    }
}
