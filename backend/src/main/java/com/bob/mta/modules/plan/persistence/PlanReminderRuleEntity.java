package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanReminderTrigger;

import java.util.List;

public record PlanReminderRuleEntity(
        String planId,
        String ruleId,
        PlanReminderTrigger trigger,
        int offsetMinutes,
        List<String> channels,
        String templateId,
        List<String> recipients,
        String description
) {

    public PlanReminderRuleEntity {
        channels = channels == null ? List.of() : List.copyOf(channels);
        recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }
}
