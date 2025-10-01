package com.bob.mta.modules.file.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMetadataMapper {

    void insert(FileMetadataEntity entity);

    FileMetadataEntity findById(@Param("id") String id);

    List<FileMetadataEntity> findByBiz(@Param("bizType") String bizType, @Param("bizId") String bizId);

    void delete(@Param("id") String id);
}
