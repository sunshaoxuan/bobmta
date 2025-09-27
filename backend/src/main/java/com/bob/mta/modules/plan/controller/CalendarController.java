package com.bob.mta.modules.plan.controller;

import com.bob.mta.modules.plan.service.PlanService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final PlanService planService;

    public CalendarController(PlanService planService) {
        this.planService = planService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping(value = "/tenant/{tenantId}.ics", produces = "text/calendar")
    public ResponseEntity<String> tenantFeed(@PathVariable String tenantId) {
        String content = planService.renderTenantCalendar(tenantId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(content);
    }
}
