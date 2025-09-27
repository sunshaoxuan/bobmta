package com.bob.mta.modules.audit.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.audit.domain.AuditLog;
import com.bob.mta.modules.audit.dto.AuditLogResponse;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditControllerTest {

    @Test
    void shouldListAuditLogs() {
        InMemoryAuditService auditService = new InMemoryAuditService();
        auditService.record(new AuditLog(0L, OffsetDateTime.now(), "user", "user", "Entity", "1", "ACTION",
                "detail", null, null, "req", null, null));
        AuditController controller = new AuditController(auditService);
        ApiResponse<java.util.List<AuditLogResponse>> response = controller.list("Entity", "1", null, null);
        assertThat(response.getData()).hasSize(1);
    }
}
