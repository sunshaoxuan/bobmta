package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.repository.PlanAnalyticsQuery;

import java.time.OffsetDateTime;

public record PlanAnalyticsQueryParameters(
        String tenantId,
        String customerId,
        String ownerId,
        OffsetDateTime plannedStartFrom,
        OffsetDateTime plannedEndTo,
        OffsetDateTime referenceTime,
        Integer upcomingLimit,
        Integer ownerLimit,
        Integer riskLimit,
        OffsetDateTime dueSoonThreshold
) {

    public static PlanAnalyticsQueryParameters fromQuery(PlanAnalyticsQuery query) {
        if (query == null) {
            OffsetDateTime reference = OffsetDateTime.now();
            return new PlanAnalyticsQueryParameters(null, null, null, null, null, reference, 5, 5, 5,
                    reference.plusMinutes(1440));
        }
        OffsetDateTime reference = query.getReferenceTime();
        OffsetDateTime dueSoonThreshold = reference.plusMinutes(query.getDueSoonMinutes());
        return new PlanAnalyticsQueryParameters(
                query.getTenantId(),
                query.getCustomerId(),
                query.getOwnerId(),
                query.getFrom(),
                query.getTo(),
                reference,
                query.getUpcomingLimit(),
                query.getOwnerLimit(),
                query.getRiskLimit(),
                dueSoonThreshold
        );
    }
}
