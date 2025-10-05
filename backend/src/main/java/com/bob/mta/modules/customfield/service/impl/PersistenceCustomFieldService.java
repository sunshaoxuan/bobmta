package com.bob.mta.modules.customfield.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.tenant.TenantContext;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.customfield.domain.CustomFieldDefinition;
import com.bob.mta.modules.customfield.domain.CustomFieldType;
import com.bob.mta.modules.customfield.domain.CustomFieldValue;
import com.bob.mta.modules.customfield.persistence.CustomFieldDefinitionEntity;
import com.bob.mta.modules.customfield.persistence.CustomFieldValueEntity;
import com.bob.mta.modules.customfield.repository.CustomFieldDefinitionRepository;
import com.bob.mta.modules.customfield.service.CustomFieldService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@ConditionalOnBean(CustomFieldDefinitionRepository.class)
@Transactional
public class PersistenceCustomFieldService implements CustomFieldService {

    private final CustomFieldDefinitionRepository repository;
    private final TenantContext tenantContext;

    public PersistenceCustomFieldService(CustomFieldDefinitionRepository repository, TenantContext tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Override
    public List<CustomFieldDefinition> listDefinitions() {
        return repository.list(tenantContext.getCurrentTenantId()).stream()
                .map(this::toDomain)
                .sorted((a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()))
                .toList();
    }

    @Override
    public CustomFieldDefinition getDefinition(long id) {
        String tenantId = tenantContext.getCurrentTenantId();
        CustomFieldDefinitionEntity entity = repository.findById(tenantId, id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CUSTOM_FIELD_NOT_FOUND);
        }
        return toDomain(entity);
    }

    @Override
    public CustomFieldDefinition createDefinition(String code, String label, CustomFieldType type, boolean required, List<String> options, String description) {
        String tenantId = tenantContext.getCurrentTenantId();
        if (repository.findByCode(tenantId, code) != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    Localization.text(LocalizationKeys.Errors.CUSTOM_FIELD_CODE_EXISTS));
        }
        CustomFieldDefinitionEntity entity = new CustomFieldDefinitionEntity();
        entity.setTenantId(tenantId);
        entity.setCode(code);
        entity.setLabel(label);
        entity.setType(type);
        entity.setRequired(required);
        entity.setOptions(options);
        entity.setDescription(description);
        entity.setCreatedAt(OffsetDateTime.now());
        repository.insert(entity);
        return toDomain(repository.findById(tenantId, entity.getId()));
    }

    @Override
    public CustomFieldDefinition updateDefinition(long id, String label, CustomFieldType type, boolean required, List<String> options, String description) {
        String tenantId = tenantContext.getCurrentTenantId();
        CustomFieldDefinitionEntity existing = repository.findById(tenantId, id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.CUSTOM_FIELD_NOT_FOUND);
        }
        existing.setLabel(label);
        existing.setType(type);
        existing.setRequired(required);
        existing.setOptions(options);
        existing.setDescription(description);
        repository.update(existing);
        return toDomain(repository.findById(tenantId, id));
    }

    @Override
    public void deleteDefinition(long id) {
        String tenantId = tenantContext.getCurrentTenantId();
        CustomFieldDefinitionEntity existing = repository.findById(tenantId, id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.CUSTOM_FIELD_NOT_FOUND);
        }
        repository.deleteValuesForField(tenantId, id);
        repository.delete(tenantId, id);
    }

    @Override
    public List<CustomFieldValue> listValues(String entityId) {
        String tenantId = tenantContext.getCurrentTenantId();
        return repository.listValues(tenantId, entityId).stream()
                .map(this::toValue)
                .toList();
    }

    @Override
    public List<CustomFieldValue> updateValues(String entityId, Map<Long, String> values) {
        String tenantId = tenantContext.getCurrentTenantId();
        Map<Long, CustomFieldDefinitionEntity> definitions = repository.list(tenantId).stream()
                .collect(Collectors.toMap(CustomFieldDefinitionEntity::getId, def -> def));
        Map<Long, CustomFieldValueEntity> current = repository.listValues(tenantId, entityId).stream()
                .collect(Collectors.toMap(CustomFieldValueEntity::fieldId, value -> value, (a, b) -> b, HashMap::new));
        validateRequiredFields(definitions, values, current);
        List<CustomFieldValue> updated = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<Long, String> entry : values.entrySet()) {
            Long fieldId = entry.getKey();
            String rawValue = entry.getValue();
            CustomFieldDefinitionEntity definition = definitions.get(fieldId);
            if (definition == null) {
                continue;
            }
            validateValue(definition, rawValue);
            CustomFieldValueEntity entity = new CustomFieldValueEntity(fieldId, tenantId, entityId, rawValue, now);
            repository.upsertValue(entity);
            updated.add(toValue(entity));
        }
        return updated;
    }

    private void validateRequiredFields(Map<Long, CustomFieldDefinitionEntity> definitions,
                                         Map<Long, String> requested,
                                         Map<Long, CustomFieldValueEntity> current) {
        for (CustomFieldDefinitionEntity definition : definitions.values()) {
            if (!definition.isRequired()) {
                continue;
            }
            String incoming = requested.get(definition.getId());
            if (StringUtils.hasText(incoming)) {
                continue;
            }
            CustomFieldValueEntity existing = current.get(definition.getId());
            if (existing == null || !StringUtils.hasText(existing.value())) {
                throw new BusinessException(ErrorCode.CUSTOM_FIELD_VALUE_INVALID,
                        Localization.text(LocalizationKeys.Errors.CUSTOM_FIELD_REQUIRED_EMPTY,
                                definition.getCode()));
            }
        }
    }

    private void validateValue(CustomFieldDefinitionEntity definition, String value) {
        if (!StringUtils.hasText(value)) {
            if (definition.isRequired()) {
                throw new BusinessException(ErrorCode.CUSTOM_FIELD_VALUE_INVALID,
                        Localization.text(LocalizationKeys.Errors.CUSTOM_FIELD_VALUE_REQUIRED));
            }
            return;
        }
        try {
            switch (definition.getType()) {
                case NUMBER -> Double.parseDouble(value);
                case DATE -> java.time.LocalDate.parse(value);
                case BOOLEAN -> {
                    if (!Objects.equals(value, "true") && !Objects.equals(value, "false")) {
                        throw new BusinessException(ErrorCode.CUSTOM_FIELD_VALUE_INVALID,
                                Localization.text(LocalizationKeys.Errors.CUSTOM_FIELD_BOOLEAN_EXPECTED));
                    }
                }
                case TEXT -> {
                    // no-op
                }
            }
        } catch (NumberFormatException | java.time.format.DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.CUSTOM_FIELD_VALUE_INVALID,
                    Localization.text(LocalizationKeys.Errors.CUSTOM_FIELD_VALUE_INVALID_FORMAT));
        }
        if (definition.getOptions() != null && !definition.getOptions().isEmpty()
                && definition.getOptions().stream().noneMatch(option -> option.equalsIgnoreCase(value))) {
            throw new BusinessException(ErrorCode.CUSTOM_FIELD_VALUE_INVALID,
                    Localization.text(LocalizationKeys.Errors.CUSTOM_FIELD_VALUE_INVALID_OPTION));
        }
    }

    private CustomFieldDefinition toDomain(CustomFieldDefinitionEntity entity) {
        return new CustomFieldDefinition(
                entity.getId(),
                entity.getCode(),
                entity.getLabel(),
                entity.getType(),
                entity.isRequired(),
                entity.getOptions(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    private CustomFieldValue toValue(CustomFieldValueEntity entity) {
        return new CustomFieldValue(entity.fieldId(), entity.entityId(), entity.value(), entity.updatedAt());
    }
}
