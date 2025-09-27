package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderSchedule;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;

import java.time.OffsetDateTime;
import java.util.List;

public class PlanReminderPreviewResponse {

    private final String planId;
    private final String ruleId;
    private final PlanReminderTrigger trigger;
    private final OffsetDateTime fireTime;
    private final List<String> channels;
    private final String templateId;
    private final List<String> recipients;
    private final String description;

    public PlanReminderPreviewResponse(String planId, String ruleId, PlanReminderTrigger trigger,
                                        OffsetDateTime fireTime, List<String> channels, String templateId,
                                        List<String> recipients, String description) {
        this.planId = planId;
        this.ruleId = ruleId;
        this.trigger = trigger;
        this.fireTime = fireTime;
        this.channels = channels;
        this.templateId = templateId;
        this.recipients = recipients;
        this.description = description;
    }

    public static PlanReminderPreviewResponse from(PlanReminderSchedule schedule) {
        PlanReminderRule rule = schedule.getRule();
        return new PlanReminderPreviewResponse(
                schedule.getPlanId(),
                rule.getId(),
                rule.getTrigger(),
                schedule.getFireTime(),
                rule.getChannels(),
                rule.getTemplateId(),
                rule.getRecipients(),
                rule.getDescription()
        );
    }

    public String getPlanId() {
        return planId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public PlanReminderTrigger getTrigger() {
        return trigger;
    }

    public OffsetDateTime getFireTime() {
        return fireTime;
    }

    public List<String> getChannels() {
        return channels;
    }

    public String getTemplateId() {
        return templateId;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public String getDescription() {
        return description;
    }
}
