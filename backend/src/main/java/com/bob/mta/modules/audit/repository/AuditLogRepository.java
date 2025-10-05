package com.bob.mta.modules.audit.repository;

import com.bob.mta.modules.audit.persistence.AuditLogEntity;

import java.util.List;

public interface AuditLogRepository {

    void insert(AuditLogEntity entity);

    List<AuditLogEntity> query(String tenantId, String entityType, String entityId, String action, String userId);
}
