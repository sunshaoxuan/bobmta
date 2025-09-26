package com.bob.mta.modules.plan.service;

<<<<<<< HEAD
import com.bob.mta.modules.plan.domain.Plan;

import java.util.List;

public interface PlanService {

    List<Plan> listPlans(String customerId, String status);

    Plan getPlan(String id);
=======
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;

/**
 * Maintenance plan operations.
 */
public interface PlanService {

    PageResponse<PlanSummaryResponse> listPlans(int page, int pageSize, String customerId, String status);

    PlanDetailResponse getPlan(String id);
>>>>>>> origin/main
}
