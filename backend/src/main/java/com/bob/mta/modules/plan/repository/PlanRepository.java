package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepository {

    List<Plan> findAll();

    Optional<Plan> findById(String id);

    void save(Plan plan);

    void delete(String id);

    String nextPlanId();

    String nextNodeId();

    String nextReminderId();
}
