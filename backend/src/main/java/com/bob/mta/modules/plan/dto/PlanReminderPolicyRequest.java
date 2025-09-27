package com.bob.mta.modules.plan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PlanReminderPolicyRequest {

    @Valid
    @NotNull
    private List<PlanReminderRuleRequest> rules;

    public List<PlanReminderRuleRequest> getRules() {
        return rules;
    }

    public void setRules(List<PlanReminderRuleRequest> rules) {
        this.rules = rules;
    }
}
