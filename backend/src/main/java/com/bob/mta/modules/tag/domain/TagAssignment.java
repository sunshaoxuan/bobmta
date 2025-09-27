package com.bob.mta.modules.tag.domain;

public class TagAssignment {

    private final long tagId;
    private final TagEntityType entityType;
    private final String entityId;

    public TagAssignment(long tagId, TagEntityType entityType, String entityId) {
        this.tagId = tagId;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public long getTagId() {
        return tagId;
    }

    public TagEntityType getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
