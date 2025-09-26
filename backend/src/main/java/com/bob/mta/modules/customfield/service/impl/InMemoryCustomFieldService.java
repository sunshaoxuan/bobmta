package com.bob.mta.modules.customfield.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.customfield.domain.CustomFieldDefinition;
import com.bob.mta.modules.customfield.domain.CustomFieldType;
import com.bob.mta.modules.customfield.domain.CustomFieldValue;
import com.bob.mta.modules.customfield.service.CustomFieldService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryCustomFieldService implements CustomFieldService {

    private final AtomicLong idGenerator = new AtomicLong(200);
    private final Map<Long, CustomFieldDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, CustomFieldValue>> valuesByEntity = new ConcurrentHashMap<>();

    public InMemoryCustomFieldService() {
        seedDefaults();
    }

    private void seedDefaults() {
        createDefinition("erp_version", "ERP版本", CustomFieldType.TEXT, true, List.of(), "客户ERP系统版本");
        createDefinition("critical_system", "核心系统", CustomFieldType.TEXT, false, List.of(), "关键系统名称");
    }

    @Override
    public List<CustomFieldDefinition> listDefinitions() {
        return definitions.values().stream()
                .sorted((a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()))
                .toList();
    }

    @Override
    public CustomFieldDefinition getDefinition(long id) {
        CustomFieldDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new BusinessException(ErrorCode.CUSTOM_FIELD_NOT_FOUND);
        }
        return definition;
    }

    @Override
    public CustomFieldDefinition createDefinition(String code, String label, CustomFieldType type, boolean required,
                                                   List<String> options, String description) {
        boolean exists = definitions.values().stream().anyMatch(def -> def.getCode().equalsIgnoreCase(code));
        if (exists) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Custom field code already exists");
        }
        long id = idGenerator.incrementAndGet();
        CustomFieldDefinition definition = new CustomFieldDefinition(
                id,
                code,
                label,
                type,
                required,
                options,
                description,
                OffsetDateTime.now());
        definitions.put(id, definition);
        return definition;
    }

    @Override
    public CustomFieldDefinition updateDefinition(long id, String label, CustomFieldType type, boolean required,
                                                   List<String> options, String description) {
        CustomFieldDefinition definition = getDefinition(id);
        CustomFieldDefinition updated = new CustomFieldDefinition(
                definition.getId(),
                definition.getCode(),
                label,
                type,
                required,
                options,
                description,
                definition.getCreatedAt());
        definitions.put(id, updated);
        return updated;
    }

    @Override
    public void deleteDefinition(long id) {
        definitions.remove(id);
        valuesByEntity.values().forEach(map -> map.remove(id));
    }

    @Override
    public List<CustomFieldValue> listValues(String entityId) {
        Map<Long, CustomFieldValue> values = valuesByEntity.get(entityId);
        if (values == null) {
            return List.of();
        }
        return new ArrayList<>(values.values());
    }

    @Override
    public List<CustomFieldValue> updateValues(String entityId, Map<Long, String> values) {
        Map<Long, CustomFieldValue> current = valuesByEntity.computeIfAbsent(entityId, key -> new ConcurrentHashMap<>());
        validateRequiredFields(values, current);
        List<CustomFieldValue> updatedValues = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<Long, String> entry : values.entrySet()) {
            long fieldId = entry.getKey();
            String rawValue = entry.getValue();
            CustomFieldDefinition definition = getDefinition(fieldId);
            validateValue(definition, rawValue);
            CustomFieldValue value = new CustomFieldValue(fieldId, entityId, rawValue, now);
            current.put(fieldId, value);
            updatedValues.add(value);
        }
        valuesByEntity.put(entityId, current);
        return updatedValues;
    }

    private void validateRequiredFields(Map<Long, String> newValues, Map<Long, CustomFieldValue> existing) {
        for (CustomFieldDefinition definition : definitions.values()) {
            if (!definition.isRequired()) {
                continue;
            }
            String incoming = newValues.get(definition.getId());
            if (StringUtils.hasText(incoming)) {
                continue;
            }
            CustomFieldValue current = existing.get(definition.getId());
            if (current == null || !StringUtils.hasText(current.getValue())) {
                throw new BusinessException(ErrorCode.CUSTOM_FIELD_VALUE_INVALID,
                        "Required field " + definition.getCode() + " must not be empty");
            }
        }
    }

    private void validateValue(CustomFieldDefinition definition, String value) {
        if (!StringUtils.hasText(value)) {
            if (definition.isRequired()) {
                throw new BusinessException(ErrorCode.CUSTOM_FIELD_VALUE_INVALID, "Value required");
            }
            return;
        }
        try {
            switch (definition.getType()) {
                case NUMBER -> Double.parseDouble(value);
                case DATE -> LocalDate.parse(value);
                case BOOLEAN -> {
                    if (!Objects.equals(value, "true") && !Objects.equals(value, "false")) {
                        throw new IllegalArgumentException("Boolean value must be true or false");
                    }
                }
                case TEXT -> {
                    // no-op
                }
            }
            if (!definition.getOptions().isEmpty() && definition.getOptions().stream()
                    .noneMatch(option -> option.equalsIgnoreCase(value))) {
                throw new BusinessException(ErrorCode.CUSTOM_FIELD_VALUE_INVALID, "Value not in allowed options");
            }
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.CUSTOM_FIELD_VALUE_INVALID, ex.getMessage());
        }
    }
}
