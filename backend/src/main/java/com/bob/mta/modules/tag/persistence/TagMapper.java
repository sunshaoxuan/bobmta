package com.bob.mta.modules.tag.persistence;

import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {

    List<TagDefinitionEntity> list(
            @Param("tenantId") String tenantId,
            @Param("scope") TagScope scope);

    TagDefinitionEntity findById(
            @Param("tenantId") String tenantId,
            @Param("id") long id);

    void insert(TagDefinitionEntity entity);

    int update(TagDefinitionEntity entity);

    void delete(
            @Param("tenantId") String tenantId,
            @Param("id") long id);

    void insertAssignment(TagAssignmentEntity entity);

    int deleteAssignment(TagAssignmentEntity entity);

    void deleteAssignmentsForTag(
            @Param("tenantId") String tenantId,
            @Param("tagId") long tagId);

    List<TagAssignmentEntity> listAssignments(
            @Param("tenantId") String tenantId,
            @Param("tagId") long tagId);

    List<TagDefinitionEntity> findByEntity(
            @Param("tenantId") String tenantId,
            @Param("entityType") TagEntityType entityType,
            @Param("entityId") String entityId);
}
