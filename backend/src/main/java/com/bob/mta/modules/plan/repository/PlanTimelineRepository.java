package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanActivity;

import java.util.List;

public interface PlanTimelineRepository {

    List<PlanActivity> findTimeline(String planId);

    void replaceTimeline(String planId, List<PlanActivity> activities);
}
