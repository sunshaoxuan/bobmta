package com.bob.mta.modules.audit.service.impl;

import com.bob.mta.modules.audit.domain.AuditLog;
import com.bob.mta.modules.audit.service.AuditQuery;
import com.bob.mta.modules.audit.service.AuditService;
import com.bob.mta.modules.audit.repository.AuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnMissingBean(AuditLogRepository.class)
public class InMemoryAuditService implements AuditService {

    private final AtomicLong idGenerator = new AtomicLong(0);
    private final List<AuditLog> logs = new CopyOnWriteArrayList<>();

    @Override
    public AuditLog record(AuditLog log) {
        AuditLog stored = new AuditLog(
                idGenerator.incrementAndGet(),
                log.getTimestamp(),
                log.getUserId(),
                log.getUsername(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getDetail(),
                log.getOldData(),
                log.getNewData(),
                log.getRequestId(),
                log.getIpAddress(),
                log.getUserAgent()
        );
        logs.add(stored);
        return stored;
    }

    @Override
    public List<AuditLog> query(AuditQuery query) {
        return logs.stream()
                .filter(log -> query.getEntityType() == null || query.getEntityType().equals(log.getEntityType()))
                .filter(log -> query.getEntityId() == null || query.getEntityId().equals(log.getEntityId()))
                .filter(log -> query.getAction() == null || query.getAction().equals(log.getAction()))
                .filter(log -> query.getUserId() == null || query.getUserId().equals(log.getUserId()))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .toList();
    }
}
