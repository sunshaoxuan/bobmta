package com.bob.mta.modules.customfield.dto;

import com.bob.mta.modules.customfield.domain.CustomFieldDefinition;
import com.bob.mta.modules.customfield.domain.CustomFieldType;

import java.time.OffsetDateTime;
import java.util.List;

public class CustomFieldDefinitionResponse {

    private final long id;
    private final String code;
    private final String label;
    private final CustomFieldType type;
    private final boolean required;
    private final List<String> options;
    private final String description;
    private final OffsetDateTime createdAt;

    public CustomFieldDefinitionResponse(long id, String code, String label, CustomFieldType type, boolean required,
                                         List<String> options, String description, OffsetDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.label = label;
        this.type = type;
        this.required = required;
        this.options = options;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static CustomFieldDefinitionResponse from(CustomFieldDefinition definition) {
        return new CustomFieldDefinitionResponse(
                definition.getId(),
                definition.getCode(),
                definition.getLabel(),
                definition.getType(),
                definition.isRequired(),
                definition.getOptions(),
                definition.getDescription(),
                definition.getCreatedAt()
        );
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public CustomFieldType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
