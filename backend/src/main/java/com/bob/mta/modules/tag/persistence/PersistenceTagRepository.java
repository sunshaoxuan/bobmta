package com.bob.mta.modules.tag.persistence;

import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
import com.bob.mta.modules.tag.repository.TagRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnBean(TagMapper.class)
public class PersistenceTagRepository implements TagRepository {

    private final TagMapper mapper;

    public PersistenceTagRepository(TagMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<TagDefinitionEntity> list(String tenantId, TagScope scope) {
        return mapper.list(tenantId, scope);
    }

    @Override
    public TagDefinitionEntity findById(String tenantId, long id) {
        return mapper.findById(tenantId, id);
    }

    @Override
    public void insert(TagDefinitionEntity entity) {
        mapper.insert(entity);
    }

    @Override
    public int update(TagDefinitionEntity entity) {
        return mapper.update(entity);
    }

    @Override
    public void delete(String tenantId, long id) {
        mapper.delete(tenantId, id);
    }

    @Override
    public void deleteAssignmentsForTag(String tenantId, long tagId) {
        mapper.deleteAssignmentsForTag(tenantId, tagId);
    }

    @Override
    public void insertAssignment(TagAssignmentEntity entity) {
        mapper.insertAssignment(entity);
    }

    @Override
    public int deleteAssignment(TagAssignmentEntity entity) {
        return mapper.deleteAssignment(entity);
    }

    @Override
    public List<TagAssignmentEntity> listAssignments(String tenantId, long tagId) {
        return mapper.listAssignments(tenantId, tagId);
    }

    @Override
    public List<TagDefinitionEntity> findByEntity(String tenantId, TagEntityType entityType, String entityId) {
        return mapper.findByEntity(tenantId, entityType, entityId);
    }
}
