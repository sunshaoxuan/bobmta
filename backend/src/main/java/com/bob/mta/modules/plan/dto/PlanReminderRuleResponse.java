package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;

import java.util.List;

public class PlanReminderRuleResponse {

    private final String id;
    private final PlanReminderTrigger trigger;
    private final int offsetMinutes;
    private final List<String> channels;
    private final String templateId;
    private final List<String> recipients;
    private final String description;

    public PlanReminderRuleResponse(String id, PlanReminderTrigger trigger, int offsetMinutes,
                                    List<String> channels, String templateId, List<String> recipients,
                                    String description) {
        this.id = id;
        this.trigger = trigger;
        this.offsetMinutes = offsetMinutes;
        this.channels = channels;
        this.templateId = templateId;
        this.recipients = recipients;
        this.description = description;
    }

    public static PlanReminderRuleResponse from(PlanReminderRule rule) {
        return new PlanReminderRuleResponse(
                rule.getId(),
                rule.getTrigger(),
                rule.getOffsetMinutes(),
                rule.getChannels(),
                rule.getTemplateId(),
                rule.getRecipients(),
                rule.getDescription()
        );
    }

    public String getId() {
        return id;
    }

    public PlanReminderTrigger getTrigger() {
        return trigger;
    }

    public int getOffsetMinutes() {
        return offsetMinutes;
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
