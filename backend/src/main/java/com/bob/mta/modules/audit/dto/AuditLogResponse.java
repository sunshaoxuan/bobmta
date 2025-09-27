package com.bob.mta.modules.audit.dto;

import com.bob.mta.modules.audit.domain.AuditLog;

import java.time.OffsetDateTime;

public class AuditLogResponse {

    private final long id;
    private final OffsetDateTime timestamp;
    private final String userId;
    private final String username;
    private final String entityType;
    private final String entityId;
    private final String action;
    private final String detail;
    private final String oldData;
    private final String newData;
    private final String requestId;
    private final String ipAddress;
    private final String userAgent;

    public AuditLogResponse(long id, OffsetDateTime timestamp, String userId, String username, String entityType,
                            String entityId, String action, String detail, String oldData, String newData,
                            String requestId, String ipAddress, String userAgent) {
        this.id = id;
        this.timestamp = timestamp;
        this.userId = userId;
        this.username = username;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.detail = detail;
        this.oldData = oldData;
        this.newData = newData;
        this.requestId = requestId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
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
    }

    public long getId() {
        return id;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public String getOldData() {
        return oldData;
    }

    public String getNewData() {
        return newData;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
