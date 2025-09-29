package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PlanReminderRuleRequest {

    private String id;

    @NotNull
    private PlanReminderTrigger trigger;

    @Min(0)
    private int offsetMinutes;

    @NotEmpty
    private List<String> channels;

    @NotNull
    private String templateId;

    private List<String> recipients;

    private String description;

    private Boolean active;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PlanReminderTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(PlanReminderTrigger trigger) {
        this.trigger = trigger;
    }

    public int getOffsetMinutes() {
        return offsetMinutes;
    }

    public void setOffsetMinutes(int offsetMinutes) {
        this.offsetMinutes = offsetMinutes;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
