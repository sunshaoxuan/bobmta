package com.bob.mta.modules.customfield.persistence;

import com.bob.mta.modules.customfield.repository.CustomFieldDefinitionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnBean(CustomFieldDefinitionMapper.class)
public class PersistenceCustomFieldDefinitionRepository implements CustomFieldDefinitionRepository {

    private final CustomFieldDefinitionMapper mapper;

    public PersistenceCustomFieldDefinitionRepository(CustomFieldDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<CustomFieldDefinitionEntity> list(String tenantId) {
        return mapper.list(tenantId);
    }

    @Override
    public CustomFieldDefinitionEntity findById(String tenantId, long id) {
        return mapper.findById(tenantId, id);
    }

    @Override
    public CustomFieldDefinitionEntity findByCode(String tenantId, String code) {
        return mapper.findByCode(tenantId, code);
    }

    @Override
    public void insert(CustomFieldDefinitionEntity entity) {
        mapper.insert(entity);
    }

    @Override
    public int update(CustomFieldDefinitionEntity entity) {
        return mapper.update(entity);
    }

    @Override
    public int delete(String tenantId, long id) {
        return mapper.delete(tenantId, id);
    }

    @Override
    public List<CustomFieldValueEntity> listValues(String tenantId, String entityId) {
        return mapper.listValues(tenantId, entityId);
    }

    @Override
    public void upsertValue(CustomFieldValueEntity entity) {
        mapper.upsertValue(entity);
    }

    @Override
    public void deleteValuesForField(String tenantId, long fieldId) {
        mapper.deleteValuesForField(tenantId, fieldId);
    }
}
