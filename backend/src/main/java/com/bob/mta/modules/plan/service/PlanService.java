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

    PlanSearchResult listPlans(String tenantId, String customerId, String owner, String keyword, PlanStatus status,
                               OffsetDateTime from, OffsetDateTime to, int page, int size);

    Plan getPlan(String id);

    Plan createPlan(CreatePlanCommand command);

    Plan updatePlan(String id, UpdatePlanCommand command);

    void deletePlan(String id);

    Plan publishPlan(String id, String operator);

    Plan cancelPlan(String id, String operator, String reason);

    Plan startNode(String planId, String nodeId, String operator);

    Plan completeNode(String planId, String nodeId, String operator, String result,
                      String log, List<String> fileIds);

    Plan handoverNode(String planId, String nodeId, String newAssignee, String comment, String operator);

    Plan handoverPlan(String planId, String newOwner, List<String> participants, String note, String operator);

    String renderPlanIcs(String planId);

    String renderTenantCalendar(String tenantId);

    List<PlanActivity> getPlanTimeline(String planId);

    Plan updateReminderPolicy(String planId, List<PlanReminderRule> rules, String operator);

    Plan updateReminderRule(String planId, String reminderId, Boolean active, Integer offsetMinutes, String operator);

    List<PlanReminderSchedule> previewReminderSchedule(String planId, OffsetDateTime referenceTime);

    PlanAnalytics getAnalytics(String tenantId, String customerId, String ownerId,
                               OffsetDateTime from, OffsetDateTime to,
                               OffsetDateTime referenceTime,
                               Integer upcomingLimit,
                               Integer ownerLimit,
                               Integer riskLimit,
                               Integer dueSoonMinutes);

    List<PlanActivityDescriptor> describeActivities();

    PlanReminderConfigurationDescriptor describeReminderOptions();

    PlanFilterDescriptor describePlanFilters(String tenantId);
}
