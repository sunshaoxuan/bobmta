package com.bob.mta.modules.plan.domain;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class PlanReminderPolicy {

    private final List<PlanReminderRule> rules;
    private final OffsetDateTime updatedAt;
    private final String updatedBy;

    public PlanReminderPolicy(List<PlanReminderRule> rules, OffsetDateTime updatedAt, String updatedBy) {
        this.rules = rules == null ? List.of() : List.copyOf(rules);
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static PlanReminderPolicy empty() {
        return new PlanReminderPolicy(List.of(), null, null);
    }

    public PlanReminderPolicy withRules(List<PlanReminderRule> newRules, OffsetDateTime updatedAt, String updatedBy) {
        return new PlanReminderPolicy(newRules, updatedAt, updatedBy);
    }

    public List<PlanReminderRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
