package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanActivityType;

import java.time.OffsetDateTime;
import java.util.Map;

public class PlanActivityResponse {

    private final PlanActivityType type;
    private final OffsetDateTime occurredAt;
    private final String actor;
    private final String message;
    private final String referenceId;
    private final Map<String, String> attributes;

    public PlanActivityResponse(PlanActivityType type, OffsetDateTime occurredAt, String actor, String message,
                                String referenceId, Map<String, String> attributes) {
        this.type = type;
        this.occurredAt = occurredAt;
        this.actor = actor;
        this.message = message;
        this.referenceId = referenceId;
        this.attributes = attributes;
    }

    public static PlanActivityResponse from(PlanActivity activity) {
        return new PlanActivityResponse(
                activity.getType(),
                activity.getOccurredAt(),
                activity.getActor(),
                activity.getMessage(),
                activity.getReferenceId(),
                activity.getAttributes()
        );
    }

    public PlanActivityType getType() {
        return type;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public String getMessage() {
        return message;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
