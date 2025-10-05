package com.bob.mta.modules.customer.persistence;

import java.time.OffsetDateTime;

public record CustomerEntity(
        String id,
        String tenantId,
        String code,
        String name,
        String shortName,
        String groupName,
        String region,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
