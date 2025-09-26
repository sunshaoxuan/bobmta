package com.bob.mta.modules.audit.service.impl;

import com.bob.mta.modules.audit.domain.AuditLog;
import com.bob.mta.modules.audit.service.AuditQuery;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAuditServiceTest {

    private final InMemoryAuditService service = new InMemoryAuditService();

    @Test
    void shouldStoreAuditLog() {
        AuditLog log = new AuditLog(0L, OffsetDateTime.now(), "user", "user", "Customer", "cust",
                "CREATE", "detail", null, null, "req", null, null);
        service.record(log);
        assertThat(service.query(new AuditQuery("Customer", "cust", null, null))).hasSize(1);
    }
}
