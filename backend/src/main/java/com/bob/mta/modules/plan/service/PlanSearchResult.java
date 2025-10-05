package com.bob.mta.modules.plan.service;

import com.bob.mta.modules.plan.domain.Plan;

import java.util.List;

public record PlanSearchResult(List<Plan> plans, int totalCount) {

    public PlanSearchResult {
        if (plans == null) {
            plans = List.of();
        } else {
            plans = List.copyOf(plans);
        }
        if (totalCount < 0) {
            totalCount = 0;
        }
    }
}
