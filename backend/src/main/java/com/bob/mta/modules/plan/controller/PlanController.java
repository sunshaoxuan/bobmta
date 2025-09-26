package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.service.PlanService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API endpoints exposing plan list and detailed execution views.
 */
@RestController
@RequestMapping(path = "/api/v1/plans", produces = MediaType.APPLICATION_JSON_VALUE)
public class PlanController {

    private final PlanService planService;

    public PlanController(final PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PlanSummaryResponse>> list(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(required = false) final String customerId,
            @RequestParam(required = false) final String status) {
        return ApiResponse.success(planService.listPlans(page, pageSize, customerId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlanDetailResponse> detail(@PathVariable final String id) {
        return ApiResponse.success(planService.getPlan(id));
    }
}