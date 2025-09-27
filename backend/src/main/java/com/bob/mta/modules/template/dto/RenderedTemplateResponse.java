package com.bob.mta.modules.template.dto;

import java.util.List;

public class RenderedTemplateResponse {

    private final String subject;
    private final String content;
    private final List<String> to;
    private final List<String> cc;
    private final String endpoint;

    public RenderedTemplateResponse(String subject, String content, List<String> to, List<String> cc, String endpoint) {
        this.subject = subject;
        this.content = content;
        this.to = to;
        this.cc = cc;
        this.endpoint = endpoint;
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
}
