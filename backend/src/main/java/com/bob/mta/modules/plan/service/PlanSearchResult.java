package com.bob.mta.modules.plan.service;

import com.bob.mta.modules.plan.domain.Plan;

import java.util.List;

public record PlanSearchResult(List<Plan> plans, int totalCount) {

    public PlanSearchResult {
        List<Plan> safePlans = plans == null ? List.of() : List.copyOf(plans);
        this.plans = safePlans;
        this.totalCount = Math.max(0, totalCount);
    }
}
