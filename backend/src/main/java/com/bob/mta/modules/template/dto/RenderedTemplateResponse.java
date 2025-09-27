package com.bob.mta.modules.template.dto;

import com.bob.mta.modules.template.domain.RenderedTemplate;

import java.util.List;
import java.util.Map;

public class RenderedTemplateResponse {

    private final String subject;
    private final String content;
    private final List<String> to;
    private final List<String> cc;
    private final String endpoint;
    private final String attachmentFileName;
    private final String attachmentContent;
    private final String attachmentContentType;
    private final Map<String, String> metadata;

    public RenderedTemplateResponse(String subject, String content, List<String> to, List<String> cc, String endpoint,
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
        this.metadata = metadata;
    }

    public static RenderedTemplateResponse from(RenderedTemplate template) {
        return new RenderedTemplateResponse(
                template.getSubject(),
                template.getContent(),
                template.getTo(),
                template.getCc(),
                template.getEndpoint(),
                template.getAttachmentFileName(),
                template.getAttachmentContent(),
                template.getAttachmentContentType(),
                template.getMetadata()
        );
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
