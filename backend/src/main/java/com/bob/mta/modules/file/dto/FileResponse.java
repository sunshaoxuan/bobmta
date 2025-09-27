package com.bob.mta.modules.file.dto;

import com.bob.mta.modules.file.domain.FileMetadata;

import java.time.OffsetDateTime;

public class FileResponse {

    private final String id;
    private final String fileName;
    private final String contentType;
    private final long size;
    private final String bucket;
    private final String objectKey;
    private final String bizType;
    private final String bizId;
    private final OffsetDateTime uploadedAt;
    private final String uploader;
    private final String downloadUrl;

    public FileResponse(String id, String fileName, String contentType, long size, String bucket, String objectKey,
                        String bizType, String bizId, OffsetDateTime uploadedAt, String uploader, String downloadUrl) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.bizType = bizType;
        this.bizId = bizId;
        this.uploadedAt = uploadedAt;
        this.uploader = uploader;
        this.downloadUrl = downloadUrl;
    }

    public static FileResponse from(FileMetadata metadata, String downloadUrl) {
        return new FileResponse(
                metadata.getId(),
                metadata.getFileName(),
                metadata.getContentType(),
                metadata.getSize(),
                metadata.getBucket(),
                metadata.getObjectKey(),
                metadata.getBizType(),
                metadata.getBizId(),
                metadata.getUploadedAt(),
                metadata.getUploader(),
                downloadUrl
        );
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getBizType() {
        return bizType;
    }

    public String getBizId() {
        return bizId;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public String getUploader() {
        return uploader;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
