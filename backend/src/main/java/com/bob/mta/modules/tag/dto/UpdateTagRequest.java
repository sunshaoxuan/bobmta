package com.bob.mta.modules.tag.dto;

import com.bob.mta.common.i18n.MultilingualTextPayload;
import com.bob.mta.modules.tag.domain.TagScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateTagRequest {

    @NotNull
    private MultilingualTextPayload name;

    @NotBlank
    private String color;

    private String icon;

    @NotNull
    private TagScope scope;

    private String applyRule;

    private boolean enabled = true;

    public MultilingualTextPayload getName() {
        return name;
    }

    public void setName(MultilingualTextPayload name) {
        this.name = name;
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
}
