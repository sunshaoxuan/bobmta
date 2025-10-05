package com.bob.mta.modules.customfield.repository;

import com.bob.mta.modules.customfield.persistence.CustomFieldDefinitionEntity;
import com.bob.mta.modules.customfield.persistence.CustomFieldValueEntity;

import java.util.List;

public interface CustomFieldDefinitionRepository {

    List<CustomFieldDefinitionEntity> list(String tenantId);

    CustomFieldDefinitionEntity findById(String tenantId, long id);

    CustomFieldDefinitionEntity findByCode(String tenantId, String code);

    void insert(CustomFieldDefinitionEntity entity);

    int update(CustomFieldDefinitionEntity entity);

    int delete(String tenantId, long id);

    List<CustomFieldValueEntity> listValues(String tenantId, String entityId);

    void upsertValue(CustomFieldValueEntity entity);

    void deleteValuesForField(String tenantId, long fieldId);
}
