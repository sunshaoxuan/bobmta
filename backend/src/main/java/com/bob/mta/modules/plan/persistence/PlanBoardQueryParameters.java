package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;
import com.bob.mta.modules.plan.repository.PlanBoardWindow;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public record PlanBoardQueryParameters(String tenantId,
                                       List<String> customerIds,
                                       String ownerId,
                                       List<PlanStatus> statuses,
                                       OffsetDateTime from,
                                       OffsetDateTime to,
                                       PlanBoardGrouping grouping) {

    public static PlanBoardQueryParameters from(String tenantId, PlanBoardWindow window, PlanBoardGrouping grouping) {
        PlanBoardWindow effectiveWindow = window == null ? PlanBoardWindow.builder().build() : window;
        PlanBoardGrouping effectiveGrouping = grouping == null ? PlanBoardGrouping.WEEK : grouping;
        List<String> customers = effectiveWindow.hasCustomerFilter()
                ? List.copyOf(effectiveWindow.getCustomerIds())
                : List.of();
        List<PlanStatus> statuses = effectiveWindow.getStatuses().isEmpty()
                ? List.of()
                : List.copyOf(effectiveWindow.getStatuses());
        return new PlanBoardQueryParameters(
                tenantId,
                customers,
                effectiveWindow.getOwnerId(),
                statuses,
                effectiveWindow.getFrom(),
                effectiveWindow.getTo(),
                effectiveGrouping
        );
    }

    public List<String> customerIds() {
        return customerIds == null ? List.of() : Collections.unmodifiableList(customerIds);
    }

    public List<PlanStatus> statuses() {
        return statuses == null ? List.of() : Collections.unmodifiableList(statuses);
    }
}

