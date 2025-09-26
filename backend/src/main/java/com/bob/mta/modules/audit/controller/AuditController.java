package com.bob.mta.modules.audit.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.audit.dto.AuditLogResponse;
import com.bob.mta.modules.audit.service.AuditQuery;
import com.bob.mta.modules.audit.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AuditLogResponse>> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userId) {
        AuditQuery query = new AuditQuery(entityType, entityId, action, userId);
        List<AuditLogResponse> responses = auditService.query(query).stream()
                .map(AuditLogResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}
