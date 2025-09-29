package com.bob.mta.common.i18n.persistence;

import com.bob.mta.common.i18n.MultilingualTextScope;

import java.time.OffsetDateTime;
import java.util.Map;

public record MultilingualTextEntity(
        MultilingualTextScope scope,
        String entityId,
        String field,
        String defaultLocale,
        Map<String, String> translations,
        OffsetDateTime updatedAt
) {
}
