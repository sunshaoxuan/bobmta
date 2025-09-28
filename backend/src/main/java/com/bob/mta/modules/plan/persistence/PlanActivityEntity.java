package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanActivityType;

import java.time.OffsetDateTime;
import java.util.Map;

public record PlanActivityEntity(
        String planId,
        String activityId,
        PlanActivityType type,
        OffsetDateTime occurredAt,
        String actor,
        String message,
        String referenceId,
        Map<String, String> attributes
) {

    public PlanActivityEntity {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
