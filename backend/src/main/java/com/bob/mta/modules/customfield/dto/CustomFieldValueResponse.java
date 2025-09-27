package com.bob.mta.modules.customfield.dto;

import com.bob.mta.modules.customfield.domain.CustomFieldValue;

import java.time.OffsetDateTime;

public class CustomFieldValueResponse {

    private final long fieldId;
    private final String entityId;
    private final String value;
    private final OffsetDateTime updatedAt;

    public CustomFieldValueResponse(long fieldId, String entityId, String value, OffsetDateTime updatedAt) {
        this.fieldId = fieldId;
        this.entityId = entityId;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public static CustomFieldValueResponse from(CustomFieldValue value) {
        return new CustomFieldValueResponse(value.getFieldId(), value.getEntityId(), value.getValue(), value.getUpdatedAt());
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
