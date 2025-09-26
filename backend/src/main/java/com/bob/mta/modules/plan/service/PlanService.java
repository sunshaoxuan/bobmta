package com.bob.mta.modules.plan.service;

import com.bob.mta.modules.plan.domain.Plan;

import java.util.List;

public interface PlanService {

    List<Plan> listPlans(String customerId, String status);

    Plan getPlan(String id);
}
