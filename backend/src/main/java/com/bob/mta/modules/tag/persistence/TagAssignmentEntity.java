package com.bob.mta.modules.tag.persistence;

import com.bob.mta.modules.tag.domain.TagEntityType;

import java.time.OffsetDateTime;

public record TagAssignmentEntity(
        Long tagId,
        String tenantId,
        TagEntityType entityType,
        String entityId,
        OffsetDateTime createdAt
) {
}
