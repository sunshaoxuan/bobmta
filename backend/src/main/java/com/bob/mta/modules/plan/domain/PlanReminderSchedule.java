package com.bob.mta.modules.plan.domain;

import java.time.OffsetDateTime;

public class PlanReminderSchedule {

    private final String planId;
    private final PlanReminderRule rule;
    private final OffsetDateTime fireTime;

    public PlanReminderSchedule(String planId, PlanReminderRule rule, OffsetDateTime fireTime) {
        this.planId = planId;
        this.rule = rule;
        this.fireTime = fireTime;
    }

    public String getPlanId() {
        return planId;
    }

    public PlanReminderRule getRule() {
        return rule;
    }

    public OffsetDateTime getFireTime() {
        return fireTime;
    }
}
