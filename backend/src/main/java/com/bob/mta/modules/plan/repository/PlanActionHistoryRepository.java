package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanActionHistory;

import java.util.List;

public interface PlanActionHistoryRepository {

    void append(PlanActionHistory history);

    List<PlanActionHistory> findByPlanId(String planId);

    void deleteByPlanId(String planId);
}
