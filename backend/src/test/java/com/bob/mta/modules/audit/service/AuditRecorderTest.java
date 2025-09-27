package com.bob.mta.modules.audit.service;

import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRecorderTest {

    private AuditRecorder recorder;
    private InMemoryAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new InMemoryAuditService();
        recorder = new AuditRecorder(auditService, new ObjectMapper());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("auditor", "pass"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRecordAuditEntry() {
        recorder.record("Entity", "1", "ACTION", "description", null, new TestPayload("value"));
        assertThat(auditService.query(new AuditQuery(null, null, null, null))).hasSize(1);
    }

    private record TestPayload(String value) {
    }
}
