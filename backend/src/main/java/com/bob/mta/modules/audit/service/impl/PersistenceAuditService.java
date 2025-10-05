package com.bob.mta.modules.audit.service.impl;

import com.bob.mta.common.tenant.TenantContext;
import com.bob.mta.modules.audit.domain.AuditLog;
import com.bob.mta.modules.audit.persistence.AuditLogEntity;
import com.bob.mta.modules.audit.persistence.AuditLogMapper;
import com.bob.mta.modules.audit.service.AuditQuery;
import com.bob.mta.modules.audit.service.AuditService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnBean(AuditLogMapper.class)
public class PersistenceAuditService implements AuditService {

    private final AuditLogMapper mapper;
    private final TenantContext tenantContext;

    public PersistenceAuditService(AuditLogMapper mapper, TenantContext tenantContext) {
        this.mapper = mapper;
        this.tenantContext = tenantContext;
    }

    @Override
    public AuditLog record(AuditLog log) {
        String tenantId = tenantContext.getCurrentTenantId();
        AuditLogEntity entity = new AuditLogEntity();
        entity.setTenantId(tenantId);
        entity.setTimestamp(log.getTimestamp());
        entity.setUserId(log.getUserId());
        entity.setUsername(log.getUsername());
        entity.setEntityType(log.getEntityType());
        entity.setEntityId(log.getEntityId());
        entity.setAction(log.getAction());
        entity.setDetail(log.getDetail());
        entity.setOldData(log.getOldData());
        entity.setNewData(log.getNewData());
        entity.setRequestId(log.getRequestId());
        entity.setIpAddress(log.getIpAddress());
        entity.setUserAgent(log.getUserAgent());
        mapper.insert(entity);
        return new AuditLog(
                entity.getId(),
                entity.getTimestamp(),
                entity.getUserId(),
                entity.getUsername(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getDetail(),
                entity.getOldData(),
                entity.getNewData(),
                entity.getRequestId(),
                entity.getIpAddress(),
                entity.getUserAgent()
        );
    }

    @Override
    public List<AuditLog> query(AuditQuery query) {
        String tenantId = tenantContext.getCurrentTenantId();
        return mapper.query(tenantId, query.getEntityType(), query.getEntityId(), query.getAction(), query.getUserId())
                .stream()
                .map(entity -> new AuditLog(
                        entity.getId(),
                        entity.getTimestamp(),
                        entity.getUserId(),
                        entity.getUsername(),
                        entity.getEntityType(),
                        entity.getEntityId(),
                        entity.getAction(),
                        entity.getDetail(),
                        entity.getOldData(),
                        entity.getNewData(),
                        entity.getRequestId(),
                        entity.getIpAddress(),
                        entity.getUserAgent()))
                .toList();
    }
}
