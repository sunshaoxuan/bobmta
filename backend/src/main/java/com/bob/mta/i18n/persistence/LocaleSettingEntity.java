package com.bob.mta.i18n.persistence;

import java.time.OffsetDateTime;

public record LocaleSettingEntity(
        String key,
        String value,
        OffsetDateTime updatedAt
) {
}

