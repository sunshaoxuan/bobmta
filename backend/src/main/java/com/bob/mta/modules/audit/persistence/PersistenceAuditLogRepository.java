package com.bob.mta.modules.audit.persistence;

import com.bob.mta.modules.audit.repository.AuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnBean(AuditLogMapper.class)
public class PersistenceAuditLogRepository implements AuditLogRepository {

    private final AuditLogMapper mapper;

    public PersistenceAuditLogRepository(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(AuditLogEntity entity) {
        mapper.insert(entity);
    }

    @Override
    public List<AuditLogEntity> query(String tenantId, String entityType, String entityId, String action, String userId) {
        return mapper.query(tenantId, entityType, entityId, action, userId);
    }
}
