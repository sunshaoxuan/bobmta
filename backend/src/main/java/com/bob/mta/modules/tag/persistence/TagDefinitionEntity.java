package com.bob.mta.modules.tag.persistence;

import com.bob.mta.modules.tag.domain.TagScope;

import java.time.OffsetDateTime;

public class TagDefinitionEntity {

    private Long id;
    private String tenantId;
    private String defaultLocale;
    private String defaultName;
    private String color;
    private String icon;
    private TagScope scope;
    private String applyRule;
    private boolean enabled;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public TagScope getScope() {
        return scope;
    }

    public void setScope(TagScope scope) {
        this.scope = scope;
    }

    public String getApplyRule() {
        return applyRule;
    }

    public void setApplyRule(String applyRule) {
        this.applyRule = applyRule;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
