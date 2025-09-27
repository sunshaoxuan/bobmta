package com.bob.mta.modules.tag.dto;

import com.bob.mta.common.i18n.MultilingualTextPayload;
import com.bob.mta.modules.tag.domain.TagDefinition;
import com.bob.mta.modules.tag.domain.TagScope;

import java.time.OffsetDateTime;

public class TagResponse {

    private final long id;
    private final MultilingualTextPayload name;
    private final String color;
    private final String icon;
    private final TagScope scope;
    private final String applyRule;
    private final boolean enabled;
    private final OffsetDateTime createdAt;

    public TagResponse(long id, MultilingualTextPayload name, String color, String icon, TagScope scope,
                       String applyRule, boolean enabled, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.scope = scope;
        this.applyRule = applyRule;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public static TagResponse from(TagDefinition definition) {
        return new TagResponse(
                definition.getId(),
                MultilingualTextPayload.fromValue(definition.getName()),
                definition.getColor(),
                definition.getIcon(),
                definition.getScope(),
                definition.getApplyRule(),
                definition.isEnabled(),
                definition.getCreatedAt()
        );
    }

    public long getId() {
        return id;
    }

    public MultilingualTextPayload getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public TagScope getScope() {
        return scope;
    }

    public String getApplyRule() {
        return applyRule;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
