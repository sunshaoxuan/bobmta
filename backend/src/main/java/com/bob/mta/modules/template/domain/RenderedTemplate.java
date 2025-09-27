package com.bob.mta.modules.template.domain;

import java.util.List;
import java.util.Map;

public class RenderedTemplate {

    private final String subject;
    private final String content;
    private final List<String> to;
    private final List<String> cc;
    private final String endpoint;
    private final String attachmentFileName;
    private final String attachmentContent;
    private final String attachmentContentType;
    private final Map<String, String> metadata;

    public RenderedTemplate(String subject, String content, List<String> to, List<String> cc, String endpoint,
                            String attachmentFileName, String attachmentContent, String attachmentContentType,
                            Map<String, String> metadata) {
        this.subject = subject;
        this.content = content;
        this.to = to;
        this.cc = cc;
        this.endpoint = endpoint;
        this.attachmentFileName = attachmentFileName;
        this.attachmentContent = attachmentContent;
        this.attachmentContentType = attachmentContentType;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
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

    public String getAttachmentFileName() {
        return attachmentFileName;
    }

    public String getAttachmentContent() {
        return attachmentContent;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
