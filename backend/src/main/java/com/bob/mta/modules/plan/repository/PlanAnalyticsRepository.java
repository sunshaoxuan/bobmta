package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanAnalytics;

public interface PlanAnalyticsRepository {

    PlanAnalytics summarize(PlanAnalyticsQuery query);
}
