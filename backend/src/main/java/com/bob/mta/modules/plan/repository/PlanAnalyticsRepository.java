package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.service.PlanBoardView;

public interface PlanAnalyticsRepository {

    PlanAnalytics summarize(PlanAnalyticsQuery query);

    PlanBoardView getPlanBoard(String tenantId, PlanBoardWindow window, PlanBoardGrouping grouping);
}
