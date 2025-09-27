package com.bob.mta.modules.template.dto;

import com.bob.mta.common.i18n.MultilingualTextPayload;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UpdateTemplateRequest {

    @NotNull
    private MultilingualTextPayload name;

    private MultilingualTextPayload subject;

    @NotNull
    private MultilingualTextPayload content;

    private List<String> to;

    private List<String> cc;

    private String endpoint;

    private boolean enabled = true;

    private MultilingualTextPayload description;

    public MultilingualTextPayload getName() {
        return name;
    }

    public void setName(MultilingualTextPayload name) {
        this.name = name;
    }

    public MultilingualTextPayload getSubject() {
        return subject;
    }

    public void setSubject(MultilingualTextPayload subject) {
        this.subject = subject;
    }

    public MultilingualTextPayload getContent() {
        return content;
    }

    public void setContent(MultilingualTextPayload content) {
        this.content = content;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
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

    public MultilingualTextPayload getDescription() {
        return description;
    }

    public void setDescription(MultilingualTextPayload description) {
        this.description = description;
    }
}
