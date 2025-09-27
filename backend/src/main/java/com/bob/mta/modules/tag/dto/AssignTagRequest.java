package com.bob.mta.modules.tag.dto;

import com.bob.mta.modules.tag.domain.TagEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AssignTagRequest {

    @NotNull
    private TagEntityType entityType;

    @NotBlank
    private String entityId;

    public TagEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(TagEntityType entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
