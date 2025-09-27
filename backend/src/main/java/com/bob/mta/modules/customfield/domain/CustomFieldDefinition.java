package com.bob.mta.modules.customfield.domain;

import java.time.OffsetDateTime;
import java.util.List;

public class CustomFieldDefinition {

    private final long id;
    private final String code;
    private final String label;
    private final CustomFieldType type;
    private final boolean required;
    private final List<String> options;
    private final String description;
    private final OffsetDateTime createdAt;

    public CustomFieldDefinition(long id, String code, String label, CustomFieldType type, boolean required,
                                 List<String> options, String description, OffsetDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.label = label;
        this.type = type;
        this.required = required;
        this.options = options == null ? List.of() : List.copyOf(options);
        this.description = description;
        this.createdAt = createdAt;
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

    public CustomFieldDefinition withLabel(String newLabel) {
        return new CustomFieldDefinition(id, code, newLabel, type, required, options, description, createdAt);
    }

    public CustomFieldDefinition withType(CustomFieldType newType) {
        return new CustomFieldDefinition(id, code, label, newType, required, options, description, createdAt);
    }

    public CustomFieldDefinition withRequired(boolean newRequired) {
        return new CustomFieldDefinition(id, code, label, type, newRequired, options, description, createdAt);
    }

    public CustomFieldDefinition withOptions(List<String> newOptions) {
        return new CustomFieldDefinition(id, code, label, type, required, newOptions, description, createdAt);
    }

    public CustomFieldDefinition withDescription(String newDescription) {
        return new CustomFieldDefinition(id, code, label, type, required, options, newDescription, createdAt);
    }
}
