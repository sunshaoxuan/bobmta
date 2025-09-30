package com.bob.mta.modules.plan.service;

import java.util.List;

public record PlanReminderConfigurationDescriptor(
        List<Option> triggers,
        List<Option> channels,
        List<Option> recipientGroups,
        int minOffsetMinutes,
        int maxOffsetMinutes,
        int defaultOffsetMinutes
) {

    public PlanReminderConfigurationDescriptor {
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
        channels = channels == null ? List.of() : List.copyOf(channels);
        recipientGroups = recipientGroups == null ? List.of() : List.copyOf(recipientGroups);
    }

    public record Option(String id, String labelKey, String descriptionKey) {
    }
}
