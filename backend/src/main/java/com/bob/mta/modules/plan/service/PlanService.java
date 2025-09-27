package com.bob.mta.modules.plan.service;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderSchedule;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.UpdatePlanCommand;

import java.time.OffsetDateTime;
import java.util.List;

public interface PlanService {

    List<Plan> listPlans(String customerId, PlanStatus status, OffsetDateTime from, OffsetDateTime to);

    Plan getPlan(String id);

    Plan createPlan(CreatePlanCommand command);

    Plan updatePlan(String id, UpdatePlanCommand command);

    void deletePlan(String id);

    Plan publishPlan(String id, String operator);

    Plan cancelPlan(String id, String operator, String reason);

    PlanNodeExecution startNode(String planId, String nodeId, String operator);

    PlanNodeExecution completeNode(String planId, String nodeId, String operator, String result,
                                   String log, List<String> fileIds);

    String renderPlanIcs(String planId);

    String renderTenantCalendar(String tenantId);

    List<PlanActivity> getPlanTimeline(String planId);

    Plan updateReminderPolicy(String planId, List<PlanReminderRule> rules, String operator);

    List<PlanReminderSchedule> previewReminderSchedule(String planId, OffsetDateTime referenceTime);

    PlanAnalytics getAnalytics(String tenantId, OffsetDateTime from, OffsetDateTime to);
}
