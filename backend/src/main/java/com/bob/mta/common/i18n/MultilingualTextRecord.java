package com.bob.mta.common.i18n;

import java.util.Objects;

public class MultilingualTextRecord {

    private final MultilingualTextScope scope;
    private final String entityId;
    private final String field;
    private final MultilingualText text;

    public MultilingualTextRecord(MultilingualTextScope scope, String entityId, String field, MultilingualText text) {
        this.scope = Objects.requireNonNull(scope, "scope is required");
        this.entityId = Objects.requireNonNull(entityId, "entityId is required");
        this.field = Objects.requireNonNull(field, "field is required");
        this.text = Objects.requireNonNull(text, "text is required");
    }

    public MultilingualTextScope getScope() {
        return scope;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getField() {
        return field;
    }

    public MultilingualText getText() {
        return text;
    }
}
