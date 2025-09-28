package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanSearchCriteria;

import java.time.OffsetDateTime;

public record PlanQueryParameters(
        String tenantId,
        String customerId,
        String owner,
        String keyword,
        PlanStatus status,
        OffsetDateTime plannedStartFrom,
        OffsetDateTime plannedEndTo,
        Integer limit,
        Integer offset
) {

    public static PlanQueryParameters empty() {
        return new PlanQueryParameters(null, null, null, null, null, null, null, null, null);
    }

    public static PlanQueryParameters fromCriteria(PlanSearchCriteria criteria) {
        if (criteria == null) {
            return empty();
        }
        return new PlanQueryParameters(
                criteria.getTenantId(),
                criteria.getCustomerId(),
                criteria.getOwner(),
                criteria.getKeyword(),
                criteria.getStatus(),
                criteria.getFrom(),
                criteria.getTo(),
                null,
                null
        );
    }
}
