package com.bob.mta.modules.customfield.domain;

import java.time.OffsetDateTime;

public class CustomFieldValue {

    private final long fieldId;
    private final String entityId;
    private final String value;
    private final OffsetDateTime updatedAt;

    public CustomFieldValue(long fieldId, String entityId, String value, OffsetDateTime updatedAt) {
        this.fieldId = fieldId;
        this.entityId = entityId;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public long getFieldId() {
        return fieldId;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getValue() {
        return value;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
