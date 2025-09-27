package com.bob.mta.modules.plan.domain;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class PlanActivity {

    private final PlanActivityType type;
    private final OffsetDateTime occurredAt;
    private final String actor;
    private final String message;
    private final String referenceId;
    private final Map<String, String> attributes;

    public PlanActivity(PlanActivityType type, OffsetDateTime occurredAt, String actor, String message,
                        String referenceId, Map<String, String> attributes) {
        this.type = Objects.requireNonNull(type, "type");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.actor = actor;
        this.message = message;
        this.referenceId = referenceId;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
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
        return Collections.unmodifiableMap(attributes);
    }
}
