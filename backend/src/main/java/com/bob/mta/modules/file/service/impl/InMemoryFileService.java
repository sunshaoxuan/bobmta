package com.bob.mta.modules.file.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.file.domain.FileMetadata;
import com.bob.mta.modules.file.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryFileService implements FileService {

    private final Map<String, FileMetadata> storage = new ConcurrentHashMap<>();

    @Override
    public FileMetadata register(String fileName, String contentType, long size, String bucket, String bizType,
                                 String bizId, String uploader) {
        String id = UUID.randomUUID().toString();
        String objectKey = id + "/" + fileName;
        FileMetadata metadata = new FileMetadata(id, fileName, contentType, size, bucket, objectKey,
                bizType, bizId, OffsetDateTime.now(), uploader);
        storage.put(id, metadata);
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
                .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                .toList();
    }

    @Override
    public void delete(String id) {
        storage.remove(id);
    }

    @Override
    public String buildDownloadUrl(FileMetadata metadata) {
        return "https://minio.local/" + metadata.getBucket() + "/" + metadata.getObjectKey();
    }
}
