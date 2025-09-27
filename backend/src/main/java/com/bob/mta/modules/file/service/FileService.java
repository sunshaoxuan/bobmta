package com.bob.mta.modules.file.service;

import com.bob.mta.modules.file.domain.FileMetadata;

import java.util.List;

public interface FileService {

    FileMetadata register(String fileName, String contentType, long size, String bucket, String bizType, String bizId,
                          String uploader);

    FileMetadata get(String id);

    List<FileMetadata> listByBiz(String bizType, String bizId);

    void delete(String id);

    String buildDownloadUrl(FileMetadata metadata);
}
