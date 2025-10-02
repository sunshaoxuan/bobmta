package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanRiskEvaluator;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;
import com.bob.mta.modules.plan.repository.PlanSearchCriteria;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public record PlanBoardQueryParameters(String tenantId,
                                       List<String> customerIds,
                                       String ownerId,
                                       List<PlanStatus> statuses,
                                       OffsetDateTime from,
                                       OffsetDateTime to,
                                       PlanBoardGrouping grouping,
                                       OffsetDateTime referenceTime,
                                       int dueSoonMinutes) {

    public static PlanBoardQueryParameters fromCriteria(PlanSearchCriteria criteria,
                                                        PlanBoardGrouping grouping,
                                                        OffsetDateTime referenceTime,
                                                        int dueSoonMinutes) {
        PlanSearchCriteria effectiveCriteria = criteria == null
                ? PlanSearchCriteria.builder().build()
                : criteria;
        PlanBoardGrouping effectiveGrouping = grouping == null ? PlanBoardGrouping.WEEK : grouping;
        OffsetDateTime reference = referenceTime == null ? OffsetDateTime.now() : referenceTime;
        int dueSoon = dueSoonMinutes <= 0 ? PlanRiskEvaluator.DEFAULT_DUE_SOON_MINUTES : dueSoonMinutes;
        List<String> customers = effectiveCriteria.getCustomerIds().isEmpty()
                ? List.of()
                : List.copyOf(effectiveCriteria.getCustomerIds());
        List<PlanStatus> statuses = effectiveCriteria.getStatuses().isEmpty()
                ? List.of()
                : List.copyOf(effectiveCriteria.getStatuses());
        return new PlanBoardQueryParameters(
                effectiveCriteria.getTenantId(),
                customers,
                effectiveCriteria.getOwner(),
                statuses,
                effectiveCriteria.getFrom(),
                effectiveCriteria.getTo(),
                effectiveGrouping,
                reference,
                dueSoon
        );
    }

    public List<String> customerIds() {
        return customerIds == null ? List.of() : Collections.unmodifiableList(customerIds);
    }

    public List<PlanStatus> statuses() {
        return statuses == null ? List.of() : Collections.unmodifiableList(statuses);
    }
}

