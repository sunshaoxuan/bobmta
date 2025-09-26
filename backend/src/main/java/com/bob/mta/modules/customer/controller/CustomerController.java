package com.bob.mta.modules.customer.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;
import com.bob.mta.modules.customer.service.CustomerService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST facade for customer domain operations.
 */
@RestController
@RequestMapping(path = "/api/v1/customers", produces = MediaType.APPLICATION_JSON_VALUE)
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(final CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CustomerSummaryResponse>> list(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(required = false) final String keyword,
            @RequestParam(required = false) final String region) {
        return ApiResponse.success(customerService.listCustomers(page, pageSize, keyword, region));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerDetailResponse> detail(@PathVariable final String id) {
        return ApiResponse.success(customerService.getCustomer(id));
    }
}
