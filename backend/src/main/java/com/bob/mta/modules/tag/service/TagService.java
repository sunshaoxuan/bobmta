package com.bob.mta.modules.tag.service;

import com.bob.mta.modules.tag.domain.TagAssignment;
import com.bob.mta.modules.tag.domain.TagDefinition;
import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;

import java.util.List;

public interface TagService {

    List<TagDefinition> list(TagScope scope);

    TagDefinition getById(long id);

    TagDefinition create(String name, String color, String icon, TagScope scope, String applyRule, boolean enabled);

    TagDefinition update(long id, String name, String color, String icon, TagScope scope, String applyRule, boolean enabled);

    void delete(long id);

    TagAssignment assign(long tagId, TagEntityType entityType, String entityId);

    void removeAssignment(long tagId, TagEntityType entityType, String entityId);

    List<TagAssignment> listAssignments(long tagId);

    List<TagDefinition> findByEntity(TagEntityType entityType, String entityId);
}
