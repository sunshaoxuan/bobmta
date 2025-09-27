package com.bob.mta.modules.customer.service;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;

/**
 * Customer module operations.
 */
public interface CustomerService {

    PageResponse<CustomerSummaryResponse> listCustomers(int page, int pageSize, String keyword, String region);

    CustomerDetailResponse getCustomer(String id);
}