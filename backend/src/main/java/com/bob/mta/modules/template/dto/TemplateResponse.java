package com.bob.mta.modules.template.dto;

import com.bob.mta.common.i18n.MultilingualTextPayload;
import com.bob.mta.modules.template.domain.TemplateDefinition;
import com.bob.mta.modules.template.domain.TemplateType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

public class TemplateResponse {

    private final long id;
    private final TemplateType type;
    private final MultilingualTextPayload name;
    private final MultilingualTextPayload subject;
    private final MultilingualTextPayload content;
    private final List<String> to;
    private final List<String> cc;
    private final String endpoint;
    private final boolean enabled;
    private final MultilingualTextPayload description;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public TemplateResponse(long id, TemplateType type, MultilingualTextPayload name, MultilingualTextPayload subject, MultilingualTextPayload content,
                            List<String> to, List<String> cc, String endpoint, boolean enabled, MultilingualTextPayload description,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.subject = subject;
        this.content = content;
        this.to = to;
        this.cc = cc;
        this.endpoint = endpoint;
        this.enabled = enabled;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TemplateResponse from(TemplateDefinition definition) {
        return from(definition, null);
    }

    public static TemplateResponse from(TemplateDefinition definition, Locale locale) {
        return new TemplateResponse(
                definition.getId(),
                definition.getType(),
                MultilingualTextPayload.fromValue(definition.getName(), locale),
                MultilingualTextPayload.fromValue(definition.getSubject(), locale),
                MultilingualTextPayload.fromValue(definition.getContent(), locale),
                definition.getTo(),
                definition.getCc(),
                definition.getEndpoint(),
                definition.isEnabled(),
                MultilingualTextPayload.fromValue(definition.getDescription(), locale),
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }

    public long getId() {
        return id;
    }

    public TemplateType getType() {
        return type;
    }

    public MultilingualTextPayload getName() {
        return name;
    }

    public MultilingualTextPayload getSubject() {
        return subject;
    }

    public MultilingualTextPayload getContent() {
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

    public MultilingualTextPayload getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
