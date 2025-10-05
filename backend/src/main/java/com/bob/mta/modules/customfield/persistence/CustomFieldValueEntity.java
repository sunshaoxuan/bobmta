package com.bob.mta.modules.customfield.persistence;

import java.time.OffsetDateTime;

public record CustomFieldValueEntity(
        Long fieldId,
        String tenantId,
        String entityId,
        String value,
        OffsetDateTime updatedAt
) {
}
