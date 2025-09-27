package com.bob.mta.modules.template.domain;

import java.time.OffsetDateTime;
import java.util.List;

public class TemplateDefinition {

    private final long id;
    private final TemplateType type;
    private final String name;
    private final String subject;
    private final String content;
    private final List<String> to;
    private final List<String> cc;
    private final String endpoint;
    private final boolean enabled;
    private final String description;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public TemplateDefinition(long id, TemplateType type, String name, String subject, String content,
                              List<String> to, List<String> cc, String endpoint, boolean enabled,
                              String description, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
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

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
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

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public TemplateDefinition withName(String newName) {
        return new TemplateDefinition(id, type, newName, subject, content, to, cc, endpoint, enabled, description,
                createdAt, OffsetDateTime.now());
    }

    public TemplateDefinition withContent(String newSubject, String newContent, List<String> newTo,
                                          List<String> newCc, String newEndpoint, boolean newEnabled,
                                          String newDescription) {
        return new TemplateDefinition(id, type, name, newSubject, newContent, newTo, newCc, newEndpoint, newEnabled,
                newDescription, createdAt, OffsetDateTime.now());
    }
}
