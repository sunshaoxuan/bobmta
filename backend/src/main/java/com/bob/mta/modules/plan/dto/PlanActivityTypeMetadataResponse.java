package com.bob.mta.modules.plan.dto;

import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.service.PlanActivityDescriptor;

import java.util.List;

public class PlanActivityTypeMetadataResponse {

    private final PlanActivityType type;
    private final List<MessageEntry> messages;
    private final List<ActivityAttributeResponse> attributes;

    public PlanActivityTypeMetadataResponse(PlanActivityType type,
                                            List<MessageEntry> messages,
                                            List<ActivityAttributeResponse> attributes) {
        this.type = type;
        this.messages = messages;
        this.attributes = attributes;
    }

    public static PlanActivityTypeMetadataResponse from(PlanActivityDescriptor descriptor,
                                                        MessageResolver resolver) {
        List<MessageEntry> messageEntries = descriptor.messageKeys().stream()
                .map(key -> new MessageEntry(key, resolver.getMessage(key)))
                .toList();
        List<ActivityAttributeResponse> attributeResponses = descriptor.attributes().stream()
                .map(attribute -> new ActivityAttributeResponse(attribute.name(), attribute.descriptionKey(),
                        resolver.getMessage(attribute.descriptionKey())))
                .toList();
        return new PlanActivityTypeMetadataResponse(descriptor.type(), messageEntries, attributeResponses);
    }

    public PlanActivityType getType() {
        return type;
    }

    public List<MessageEntry> getMessages() {
        return messages;
    }

    public List<ActivityAttributeResponse> getAttributes() {
        return attributes;
    }

    public static class MessageEntry {
        private final String key;
        private final String message;

        public MessageEntry(String key, String message) {
            this.key = key;
            this.message = message;
        }

        public String getKey() {
            return key;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ActivityAttributeResponse {
        private final String name;
        private final String descriptionKey;
        private final String description;

        public ActivityAttributeResponse(String name, String descriptionKey, String description) {
            this.name = name;
            this.descriptionKey = descriptionKey;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescriptionKey() {
            return descriptionKey;
        }

        public String getDescription() {
            return description;
        }
    }
}
