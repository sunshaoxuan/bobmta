package com.bob.mta.modules.plan.dto;

import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.modules.plan.service.PlanReminderConfigurationDescriptor;

import java.util.List;

public class PlanReminderOptionsResponse {

    private final List<Option> triggers;
    private final List<Option> channels;
    private final List<Option> recipientGroups;
    private final int minOffsetMinutes;
    private final int maxOffsetMinutes;
    private final int defaultOffsetMinutes;

    private PlanReminderOptionsResponse(List<Option> triggers,
                                        List<Option> channels,
                                        List<Option> recipientGroups,
                                        int minOffsetMinutes,
                                        int maxOffsetMinutes,
                                        int defaultOffsetMinutes) {
        this.triggers = triggers;
        this.channels = channels;
        this.recipientGroups = recipientGroups;
        this.minOffsetMinutes = minOffsetMinutes;
        this.maxOffsetMinutes = maxOffsetMinutes;
        this.defaultOffsetMinutes = defaultOffsetMinutes;
    }

    public static PlanReminderOptionsResponse from(PlanReminderConfigurationDescriptor descriptor,
                                                   MessageResolver resolver) {
        List<Option> triggers = descriptor.triggers().stream()
                .map(option -> Option.from(option, resolver))
                .toList();
        List<Option> channels = descriptor.channels().stream()
                .map(option -> Option.from(option, resolver))
                .toList();
        List<Option> recipients = descriptor.recipientGroups().stream()
                .map(option -> Option.from(option, resolver))
                .toList();
        return new PlanReminderOptionsResponse(triggers, channels, recipients,
                descriptor.minOffsetMinutes(), descriptor.maxOffsetMinutes(), descriptor.defaultOffsetMinutes());
    }

    public List<Option> getTriggers() {
        return triggers;
    }

    public List<Option> getChannels() {
        return channels;
    }

    public List<Option> getRecipientGroups() {
        return recipientGroups;
    }

    public int getMinOffsetMinutes() {
        return minOffsetMinutes;
    }

    public int getMaxOffsetMinutes() {
        return maxOffsetMinutes;
    }

    public int getDefaultOffsetMinutes() {
        return defaultOffsetMinutes;
    }

    public static class Option {
        private final String id;
        private final String label;
        private final String description;

        private Option(String id, String label, String description) {
            this.id = id;
            this.label = label;
            this.description = description;
        }

        public static Option from(PlanReminderConfigurationDescriptor.Option option, MessageResolver resolver) {
            String label = resolver.getMessage(option.labelKey());
            String description = resolver.getMessage(option.descriptionKey());
            return new Option(option.id(), label, description);
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }
}
