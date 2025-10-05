package com.bob.mta.modules.audit.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditLogMapper {

    void insert(AuditLogEntity entity);

    List<AuditLogEntity> query(
            @Param("tenantId") String tenantId,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("action") String action,
            @Param("userId") String userId);
}
