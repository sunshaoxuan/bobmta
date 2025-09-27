package com.bob.mta.modules.template.domain;

import com.bob.mta.common.i18n.MultilingualText;

import java.time.OffsetDateTime;
import java.util.List;

public class TemplateDefinition {

    private final long id;
    private final TemplateType type;
    private final MultilingualText name;
    private final MultilingualText subject;
    private final MultilingualText content;
    private final List<String> to;
    private final List<String> cc;
    private final String endpoint;
    private final boolean enabled;
    private final MultilingualText description;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public TemplateDefinition(long id, TemplateType type, MultilingualText name, MultilingualText subject, MultilingualText content,
                              List<String> to, List<String> cc, String endpoint, boolean enabled,
                              MultilingualText description, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.subject = subject;
        this.content = content;
        this.to = to == null ? List.of() : List.copyOf(to);
        this.cc = cc == null ? List.of() : List.copyOf(cc);
        this.endpoint = endpoint;
        this.enabled = enabled;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public TemplateType getType() {
        return type;
    }

    public MultilingualText getName() {
        return name;
    }

    public MultilingualText getSubject() {
        return subject;
    }

    public MultilingualText getContent() {
        return content;
    }

    public List<String> getTo() {
        return to;
    }

    public List<String> getCc() {
        return cc;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MultilingualText getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public TemplateDefinition withName(MultilingualText newName) {
        return new TemplateDefinition(id, type, newName, subject, content, to, cc, endpoint, enabled, description,
                createdAt, OffsetDateTime.now());
    }

    public TemplateDefinition withContent(MultilingualText newSubject, MultilingualText newContent, List<String> newTo,
                                          List<String> newCc, String newEndpoint, boolean newEnabled,
                                          MultilingualText newDescription) {
        return new TemplateDefinition(id, type, name, newSubject, newContent, newTo, newCc, newEndpoint, newEnabled,
                newDescription, createdAt, OffsetDateTime.now());
    }
}
