package com.bob.mta.modules.audit.service;

import com.bob.mta.modules.audit.domain.AuditLog;

import java.util.List;

public interface AuditService {

    AuditLog record(AuditLog log);

    List<AuditLog> query(AuditQuery query);
}
