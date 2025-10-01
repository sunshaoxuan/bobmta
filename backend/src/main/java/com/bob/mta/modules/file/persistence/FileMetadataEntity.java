package com.bob.mta.modules.file.persistence;

import java.time.OffsetDateTime;

public record FileMetadataEntity(
        String id,
        String fileName,
        String contentType,
        long size,
        String bucket,
        String objectKey,
        String bizType,
        String bizId,
        OffsetDateTime uploadedAt,
        String uploader
) {
}
