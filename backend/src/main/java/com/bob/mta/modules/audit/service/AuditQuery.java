package com.bob.mta.modules.audit.service;

public class AuditQuery {

    private final String entityType;
    private final String entityId;
    private final String action;
    private final String userId;

    public AuditQuery(String entityType, String entityId, String action, String userId) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.userId = userId;
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

    public String getUserId() {
        return userId;
    }
}
