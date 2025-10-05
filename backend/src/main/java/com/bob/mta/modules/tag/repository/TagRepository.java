package com.bob.mta.modules.tag.repository;

import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
import com.bob.mta.modules.tag.persistence.TagAssignmentEntity;
import com.bob.mta.modules.tag.persistence.TagDefinitionEntity;

import java.util.List;

public interface TagRepository {

    List<TagDefinitionEntity> list(String tenantId, TagScope scope);

    TagDefinitionEntity findById(String tenantId, long id);

    void insert(TagDefinitionEntity entity);

    int update(TagDefinitionEntity entity);

    void delete(String tenantId, long id);

    void deleteAssignmentsForTag(String tenantId, long tagId);

    void insertAssignment(TagAssignmentEntity entity);

    int deleteAssignment(TagAssignmentEntity entity);

    List<TagAssignmentEntity> listAssignments(String tenantId, long tagId);

    List<TagDefinitionEntity> findByEntity(String tenantId, TagEntityType entityType, String entityId);
}
