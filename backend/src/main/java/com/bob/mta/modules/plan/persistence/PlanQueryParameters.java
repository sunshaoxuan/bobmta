package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanSearchCriteria;

import java.time.OffsetDateTime;
import java.util.List;

public record PlanQueryParameters(
        String tenantId,
        String customerId,
        List<String> customerIds,
        String owner,
        String keyword,
        PlanStatus status,
        List<PlanStatus> statuses,
        OffsetDateTime plannedStartFrom,
        OffsetDateTime plannedEndTo,
        Integer limit,
        Integer offset,
        String excludePlanId
) {

    public static PlanQueryParameters empty() {
        return new PlanQueryParameters(null, null, List.of(), null, null, null, List.of(), null, null, null, null, null);
    }

    public static PlanQueryParameters fromCriteria(PlanSearchCriteria criteria) {
        if (criteria == null) {
            return empty();
        }
        return new PlanQueryParameters(
                criteria.getTenantId(),
                criteria.getCustomerId(),
                criteria.getCustomerIds(),
                criteria.getOwner(),
                criteria.getKeyword(),
                criteria.getStatus(),
                criteria.getStatuses(),
                criteria.getFrom(),
                criteria.getTo(),
                criteria.getLimit(),
                criteria.getOffset(),
                criteria.getExcludePlanId()
        );
    }
}
