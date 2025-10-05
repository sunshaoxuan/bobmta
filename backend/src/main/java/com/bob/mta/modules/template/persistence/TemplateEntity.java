package com.bob.mta.modules.template.persistence;

import com.bob.mta.modules.template.domain.TemplateType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class TemplateEntity {

    private Long id;
    private TemplateType type;
    private List<String> toRecipients;
    private List<String> ccRecipients;
    private String endpoint;
    private boolean enabled;
    private String nameDefaultLocale;
    private Map<String, String> nameTranslations;
    private String subjectDefaultLocale;
    private Map<String, String> subjectTranslations;
    private String contentDefaultLocale;
    private Map<String, String> contentTranslations;
    private String descriptionDefaultLocale;
    private Map<String, String> descriptionTranslations;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public TemplateEntity() {
    }

    public TemplateEntity(Long id, TemplateType type, List<String> toRecipients, List<String> ccRecipients,
                          String endpoint, boolean enabled, String nameDefaultLocale,
                          Map<String, String> nameTranslations, String subjectDefaultLocale,
                          Map<String, String> subjectTranslations, String contentDefaultLocale,
                          Map<String, String> contentTranslations, String descriptionDefaultLocale,
                          Map<String, String> descriptionTranslations, OffsetDateTime createdAt,
                          OffsetDateTime updatedAt) {
        this.id = id;
        this.type = type;
        this.toRecipients = toRecipients;
        this.ccRecipients = ccRecipients;
        this.endpoint = endpoint;
        this.enabled = enabled;
        this.nameDefaultLocale = nameDefaultLocale;
        this.nameTranslations = nameTranslations;
        this.subjectDefaultLocale = subjectDefaultLocale;
        this.subjectTranslations = subjectTranslations;
        this.contentDefaultLocale = contentDefaultLocale;
        this.contentTranslations = contentTranslations;
        this.descriptionDefaultLocale = descriptionDefaultLocale;
        this.descriptionTranslations = descriptionTranslations;
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

    public String getNameDefaultLocale() {
        return nameDefaultLocale;
    }

    public void setNameDefaultLocale(String nameDefaultLocale) {
        this.nameDefaultLocale = nameDefaultLocale;
    }

    public Map<String, String> getNameTranslations() {
        return nameTranslations == null ? Map.of() : nameTranslations;
    }

    public void setNameTranslations(Map<String, String> nameTranslations) {
        this.nameTranslations = nameTranslations == null ? Map.of() : Map.copyOf(nameTranslations);
    }

    public String getSubjectDefaultLocale() {
        return subjectDefaultLocale;
    }

    public void setSubjectDefaultLocale(String subjectDefaultLocale) {
        this.subjectDefaultLocale = subjectDefaultLocale;
    }

    public Map<String, String> getSubjectTranslations() {
        return subjectTranslations == null ? Map.of() : subjectTranslations;
    }

    public void setSubjectTranslations(Map<String, String> subjectTranslations) {
        this.subjectTranslations = subjectTranslations == null ? Map.of() : Map.copyOf(subjectTranslations);
    }

    public String getContentDefaultLocale() {
        return contentDefaultLocale;
    }

    public void setContentDefaultLocale(String contentDefaultLocale) {
        this.contentDefaultLocale = contentDefaultLocale;
    }

    public Map<String, String> getContentTranslations() {
        return contentTranslations == null ? Map.of() : contentTranslations;
    }

    public void setContentTranslations(Map<String, String> contentTranslations) {
        this.contentTranslations = contentTranslations == null ? Map.of() : Map.copyOf(contentTranslations);
    }

    public String getDescriptionDefaultLocale() {
        return descriptionDefaultLocale;
    }

    public void setDescriptionDefaultLocale(String descriptionDefaultLocale) {
        this.descriptionDefaultLocale = descriptionDefaultLocale;
    }

    public Map<String, String> getDescriptionTranslations() {
        return descriptionTranslations == null ? Map.of() : descriptionTranslations;
    }

    public void setDescriptionTranslations(Map<String, String> descriptionTranslations) {
        this.descriptionTranslations = descriptionTranslations == null ? Map.of() : Map.copyOf(descriptionTranslations);
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
