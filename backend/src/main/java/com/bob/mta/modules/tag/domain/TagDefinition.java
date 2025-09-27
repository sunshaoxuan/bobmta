package com.bob.mta.modules.tag.domain;

import java.time.OffsetDateTime;

public class TagDefinition {

    private final long id;
    private final String name;
    private final String color;
    private final String icon;
    private final TagScope scope;
    private final String applyRule;
    private final boolean enabled;
    private final OffsetDateTime createdAt;

    public TagDefinition(long id, String name, String color, String icon, TagScope scope,
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

    public long getId() {
        return id;
    }

    public String getName() {
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

    public TagDefinition withName(String newName) {
        return new TagDefinition(id, newName, color, icon, scope, applyRule, enabled, createdAt);
    }

    public TagDefinition withColor(String newColor) {
        return new TagDefinition(id, name, newColor, icon, scope, applyRule, enabled, createdAt);
    }

    public TagDefinition withIcon(String newIcon) {
        return new TagDefinition(id, name, color, newIcon, scope, applyRule, enabled, createdAt);
    }

    public TagDefinition withScope(TagScope newScope) {
        return new TagDefinition(id, name, color, icon, newScope, applyRule, enabled, createdAt);
    }

    public TagDefinition withApplyRule(String newApplyRule) {
        return new TagDefinition(id, name, color, icon, scope, newApplyRule, enabled, createdAt);
    }

    public TagDefinition withEnabled(boolean newEnabled) {
        return new TagDefinition(id, name, color, icon, scope, applyRule, newEnabled, createdAt);
    }
}
