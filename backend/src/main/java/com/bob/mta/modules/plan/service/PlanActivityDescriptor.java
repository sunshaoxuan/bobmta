package com.bob.mta.modules.plan.service;

import com.bob.mta.modules.plan.domain.PlanActivityType;

import java.util.List;

public record PlanActivityDescriptor(
        PlanActivityType type,
        List<String> messageKeys,
        List<ActivityAttribute> attributes
) {

    public PlanActivityDescriptor {
        messageKeys = messageKeys == null ? List.of() : List.copyOf(messageKeys);
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }

    public record ActivityAttribute(String name, String descriptionKey) {

        public ActivityAttribute {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("attribute name is required");
            }
            if (descriptionKey == null || descriptionKey.isBlank()) {
                throw new IllegalArgumentException("descriptionKey is required");
            }
        }
    }
}
