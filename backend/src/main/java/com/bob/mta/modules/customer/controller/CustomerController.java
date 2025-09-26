package com.bob.mta.modules.customer.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;
import com.bob.mta.modules.customer.service.CustomerService;
<<<<<<< HEAD
import org.springframework.security.access.prepost.PreAuthorize;
=======
import org.springframework.http.MediaType;
>>>>>>> origin/main
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

<<<<<<< HEAD
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
=======
/**
 * REST facade for customer domain operations.
 */
@RestController
@RequestMapping(path = "/api/v1/customers", produces = MediaType.APPLICATION_JSON_VALUE)
>>>>>>> origin/main
public class CustomerController {

    private final CustomerService customerService;

<<<<<<< HEAD
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping
    public ApiResponse<PageResponse<CustomerSummaryResponse>> search(@RequestParam(defaultValue = "") String keyword,
                                                                     @RequestParam(defaultValue = "") String region,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        List<CustomerSummaryResponse> all = customerService.search(keyword, region).stream()
                .map(CustomerSummaryResponse::from)
                .toList();
        int fromIndex = Math.min(page * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        List<CustomerSummaryResponse> items = all.subList(fromIndex, toIndex);
        return ApiResponse.success(PageResponse.of(items, all.size(), page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/{id}")
    public ApiResponse<CustomerDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.success(CustomerDetailResponse.from(customerService.getById(id)));
=======
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
>>>>>>> origin/main
    }
}
