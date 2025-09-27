package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanReminderPolicy;

import java.time.OffsetDateTime;
import java.util.List;

public class PlanReminderPolicyResponse {

    private final List<PlanReminderRuleResponse> rules;
    private final OffsetDateTime updatedAt;
    private final String updatedBy;

    public PlanReminderPolicyResponse(List<PlanReminderRuleResponse> rules, OffsetDateTime updatedAt, String updatedBy) {
        this.rules = rules;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static PlanReminderPolicyResponse from(PlanReminderPolicy policy) {
        List<PlanReminderRuleResponse> rules = policy.getRules().stream()
                .map(PlanReminderRuleResponse::from)
                .toList();
        return new PlanReminderPolicyResponse(rules, policy.getUpdatedAt(), policy.getUpdatedBy());
    }

    public List<PlanReminderRuleResponse> getRules() {
        return rules;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
