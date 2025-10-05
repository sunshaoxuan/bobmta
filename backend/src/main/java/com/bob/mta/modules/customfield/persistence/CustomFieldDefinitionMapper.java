package com.bob.mta.modules.customfield.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomFieldDefinitionMapper {

    List<CustomFieldDefinitionEntity> list(@Param("tenantId") String tenantId);

    CustomFieldDefinitionEntity findById(
            @Param("tenantId") String tenantId,
            @Param("id") long id);

    CustomFieldDefinitionEntity findByCode(
            @Param("tenantId") String tenantId,
            @Param("code") String code);

    void insert(CustomFieldDefinitionEntity entity);

    int update(CustomFieldDefinitionEntity entity);

    int delete(
            @Param("tenantId") String tenantId,
            @Param("id") long id);

    List<CustomFieldValueEntity> listValues(
            @Param("tenantId") String tenantId,
            @Param("entityId") String entityId);

    void upsertValue(CustomFieldValueEntity entity);

    void deleteValuesForField(
            @Param("tenantId") String tenantId,
            @Param("fieldId") long fieldId);
}
