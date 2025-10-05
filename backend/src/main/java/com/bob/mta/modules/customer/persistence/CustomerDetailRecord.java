package com.bob.mta.modules.customer.persistence;

import java.time.OffsetDateTime;
import java.util.List;

public record CustomerDetailRecord(
        String id,
        String code,
        String name,
        String shortName,
        String groupName,
        String region,
        OffsetDateTime updatedAt,
        List<String> tags
) {
}
