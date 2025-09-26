package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.service.PlanService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping
    public ApiResponse<PageResponse<PlanSummaryResponse>> list(@RequestParam(defaultValue = "") String customerId,
                                                               @RequestParam(defaultValue = "") String status,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        List<PlanSummaryResponse> all = planService.listPlans(customerId, status).stream()
                .map(PlanSummaryResponse::from)
                .toList();
        int fromIndex = Math.min(page * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        List<PlanSummaryResponse> items = all.subList(fromIndex, toIndex);
        return ApiResponse.success(PageResponse.of(items, all.size(), page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/{id}")
    public ApiResponse<PlanDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.success(PlanDetailResponse.from(planService.getPlan(id)));
    }
}
