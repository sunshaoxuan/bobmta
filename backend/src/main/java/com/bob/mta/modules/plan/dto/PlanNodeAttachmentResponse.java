package com.bob.mta.modules.plan.dto;

public class PlanNodeAttachmentResponse {

    private final String id;
    private final String name;
    private final String contentType;
    private final long size;
    private final String downloadUrl;

    public PlanNodeAttachmentResponse(String id, String name, String contentType, long size, String downloadUrl) {
        this.id = id;
        this.name = name;
        this.contentType = contentType;
        this.size = size;
        this.downloadUrl = downloadUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
