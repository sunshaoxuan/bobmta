package com.bob.mta.modules.tag.dto;

import com.bob.mta.modules.tag.domain.TagAssignment;
import com.bob.mta.modules.tag.domain.TagEntityType;

public class TagAssignmentResponse {

    private final long tagId;
    private final TagEntityType entityType;
    private final String entityId;

    public TagAssignmentResponse(long tagId, TagEntityType entityType, String entityId) {
        this.tagId = tagId;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public static TagAssignmentResponse from(TagAssignment assignment) {
        return new TagAssignmentResponse(
                assignment.getTagId(),
                assignment.getEntityType(),
                assignment.getEntityId()
        );
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
