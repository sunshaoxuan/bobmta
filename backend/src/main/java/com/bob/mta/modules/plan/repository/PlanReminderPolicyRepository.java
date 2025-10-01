package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanReminderPolicy;

import java.util.Optional;

public interface PlanReminderPolicyRepository {

    Optional<PlanReminderPolicy> findReminderPolicy(String planId);

    void replaceReminderPolicy(String planId, PlanReminderPolicy policy);
}
