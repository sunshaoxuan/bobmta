package com.bob.mta.modules.plan.domain;

import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import java.util.Collections;
import java.util.List;

public class PlanReminderRule {

    private final String id;
    private final PlanReminderTrigger trigger;
    private final int offsetMinutes;
    private final List<String> channels;
    private final String templateId;
    private final List<String> recipients;
    private final String description;

    public PlanReminderRule(String id, PlanReminderTrigger trigger, int offsetMinutes,
                            List<String> channels, String templateId, List<String> recipients,
                            String description) {
        if (trigger == null) {
            throw new IllegalArgumentException(Localization.text(LocalizationKeys.Errors.PLAN_REMINDER_TRIGGER_REQUIRED));
        }
        if (offsetMinutes < 0) {
            throw new IllegalArgumentException(Localization.text(LocalizationKeys.Errors.PLAN_REMINDER_OFFSET_NEGATIVE));
        }
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException(Localization.text(LocalizationKeys.Errors.PLAN_REMINDER_CHANNELS_REQUIRED));
        }
        this.id = id;
        this.trigger = trigger;
        this.offsetMinutes = offsetMinutes;
        this.channels = List.copyOf(channels);
        this.templateId = templateId == null ? null : templateId.trim();
        this.recipients = recipients == null ? List.of() : List.copyOf(recipients);
        this.description = description;
    }

    public PlanReminderRule withId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(Localization.text(LocalizationKeys.Errors.PLAN_REMINDER_ID_REQUIRED));
        }
        return new PlanReminderRule(id, trigger, offsetMinutes, channels, templateId, recipients, description);
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
        return Collections.unmodifiableList(channels);
    }

    public String getTemplateId() {
        return templateId;
    }

    public List<String> getRecipients() {
        return Collections.unmodifiableList(recipients);
    }

    public String getDescription() {
        return description;
    }
}
