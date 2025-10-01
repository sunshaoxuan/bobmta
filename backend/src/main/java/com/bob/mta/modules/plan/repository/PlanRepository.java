package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PlanRepository {

    List<Plan> findAll();

    List<Plan> findByCriteria(PlanSearchCriteria criteria);

    int countByCriteria(PlanSearchCriteria criteria);

    Optional<Plan> findById(String id);

    void save(Plan plan);

    void delete(String id);

    String nextPlanId();

    String nextNodeId();

    String nextReminderId();

    Optional<PlanReminderPolicy> findReminderPolicy(String planId);

    void replaceReminderPolicy(String planId, PlanReminderPolicy policy);

    List<PlanActivity> findTimeline(String planId);

    void replaceTimeline(String planId, List<PlanActivity> activities);

    Map<String, List<String>> findAttachments(String planId);

    void replaceAttachments(String planId, Map<String, List<String>> attachments);
}
