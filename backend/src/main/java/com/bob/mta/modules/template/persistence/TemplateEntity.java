package com.bob.mta.modules.template.persistence;

import com.bob.mta.modules.template.domain.TemplateType;

import java.time.OffsetDateTime;
import java.util.List;

public class TemplateEntity {

    private Long id;
    private TemplateType type;
    private List<String> toRecipients;
    private List<String> ccRecipients;
    private String endpoint;
    private boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public TemplateEntity() {
    }

    public TemplateEntity(Long id, TemplateType type, List<String> toRecipients, List<String> ccRecipients,
                          String endpoint, boolean enabled, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.type = type;
        this.toRecipients = toRecipients;
        this.ccRecipients = ccRecipients;
        this.endpoint = endpoint;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TemplateType getType() {
        return type;
    }

    public void setType(TemplateType type) {
        this.type = type;
    }

    public List<String> getToRecipients() {
        return toRecipients;
    }

    public void setToRecipients(List<String> toRecipients) {
        this.toRecipients = toRecipients;
    }

    public List<String> getCcRecipients() {
        return ccRecipients;
    }

    public void setCcRecipients(List<String> ccRecipients) {
        this.ccRecipients = ccRecipients;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
