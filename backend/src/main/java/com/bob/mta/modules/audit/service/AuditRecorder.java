package com.bob.mta.modules.audit.service;

import com.bob.mta.modules.audit.domain.AuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class AuditRecorder {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditRecorder(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public void record(String entityType, String entityId, String action, String detail, Object oldData, Object newData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null ? authentication.getName() : "system";
        String username = authentication != null ? authentication.getName() : "system";
        String oldJson = toJson(oldData);
        String newJson = toJson(newData);
        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        AuditLog log = new AuditLog(
                0L,
                OffsetDateTime.now(),
                userId,
                username,
                entityType,
                entityId,
                action,
                detail,
                oldJson,
                newJson,
                requestId,
                MDC.get("ip"),
                MDC.get("userAgent")
        );
        auditService.record(log);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return value.toString();
        }
    }
}
