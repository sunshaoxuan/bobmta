package com.bob.mta.modules.file.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.file.domain.FileMetadata;
import com.bob.mta.modules.file.service.FileService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class MinioFileService implements FileService {

    private static final Logger log = LoggerFactory.getLogger(MinioFileService.class);

    private final MinioClient minioClient;
    private final int downloadExpirySeconds;
    private final Clock clock;
    private final Map<String, FileMetadata> storage = new ConcurrentHashMap<>();
    private final Set<String> ensuredBuckets = ConcurrentHashMap.newKeySet();

    public MinioFileService(MinioClient minioClient, Duration downloadExpiry) {
        this(minioClient, downloadExpiry, Clock.systemUTC());
    }

    MinioFileService(MinioClient minioClient, Duration downloadExpiry, Clock clock) {
        if (downloadExpiry == null || downloadExpiry.isNegative() || downloadExpiry.isZero()) {
            throw new IllegalArgumentException("downloadExpiry must be positive");
        }
        this.minioClient = minioClient;
        long seconds = downloadExpiry.getSeconds();
        this.downloadExpirySeconds = (int) Math.min(seconds, Integer.MAX_VALUE);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public FileMetadata register(String fileName, String contentType, long size, String bucket, String bizType,
                                 String bizId, String uploader) {
        if (!StringUtils.hasText(bucket)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "file.bucket_required");
        }
        String id = UUID.randomUUID().toString();
        String safeFileName = sanitizeFileName(fileName);
        String objectKey = id + "/" + safeFileName;
        ensureBucket(bucket);
        FileMetadata metadata = new FileMetadata(id,
                fileName,
                contentType,
                size,
                bucket,
                objectKey,
                bizType,
                bizId,
                OffsetDateTime.now(clock),
                uploader);
        storage.put(id, metadata);
        log.debug("Registered file {} in bucket {} with key {}", id, bucket, objectKey);
        return metadata;
    }

    @Override
    public FileMetadata get(String id) {
        FileMetadata metadata = storage.get(id);
        if (metadata == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return metadata;
    }

    @Override
    public List<FileMetadata> listByBiz(String bizType, String bizId) {
        return storage.values().stream()
                .filter(meta -> !StringUtils.hasText(bizType) || bizType.equals(meta.getBizType()))
                .filter(meta -> !StringUtils.hasText(bizId) || bizId.equals(meta.getBizId()))
                .sorted((left, right) -> right.getUploadedAt().compareTo(left.getUploadedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        FileMetadata metadata = storage.get(id);
        if (metadata == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(metadata.getBucket())
                    .object(metadata.getObjectKey())
                    .build());
        } catch (ErrorResponseException e) {
            if (!"NoSuchKey".equals(e.errorResponse().code())) {
                log.error("Failed to delete object {}/{}", metadata.getBucket(), metadata.getObjectKey(), e);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "file.delete_failed");
            }
        } catch (Exception e) {
            log.error("Failed to delete object {}/{}", metadata.getBucket(), metadata.getObjectKey(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "file.delete_failed");
        }
        storage.remove(id);
    }

    @Override
    public String buildDownloadUrl(FileMetadata metadata) {
        if (metadata == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            // Ensure object exists before creating presigned url to provide early feedback.
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(metadata.getBucket())
                    .object(metadata.getObjectKey())
                    .build());
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(metadata.getBucket())
                    .object(metadata.getObjectKey())
                    .method(Method.GET)
                    .expiry(downloadExpirySeconds)
                    .build());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }
            log.error("Failed to build download URL for {}/{}", metadata.getBucket(), metadata.getObjectKey(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "file.download_failed");
        } catch (UncheckedIOException e) {
            log.error("I/O error when building download URL for {}/{}", metadata.getBucket(), metadata.getObjectKey(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "file.download_failed");
        } catch (Exception e) {
            log.error("Unexpected error when building download URL for {}/{}", metadata.getBucket(), metadata.getObjectKey(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "file.download_failed");
        }
    }

    private void ensureBucket(String bucket) {
        if (ensuredBuckets.contains(bucket)) {
            return;
        }
        if (!ensuredBuckets.add(bucket)) {
            return;
        }
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            ensuredBuckets.remove(bucket);
            log.error("Failed to ensure bucket {} exists", bucket, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "file.bucket_unavailable");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "file";
        }
        String normalized = fileName.trim();
        String sanitized = normalized.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isEmpty() ? "file" : sanitized;
    }
}
