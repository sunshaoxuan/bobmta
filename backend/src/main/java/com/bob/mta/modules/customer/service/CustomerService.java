package com.bob.mta.modules.customer.service;

<<<<<<< HEAD
import com.bob.mta.modules.customer.domain.Customer;

import java.util.List;

public interface CustomerService {

    List<Customer> search(String keyword, String region);

    Customer getById(String id);
=======
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;

/**
 * Customer module operations.
 */
public interface CustomerService {

    PageResponse<CustomerSummaryResponse> listCustomers(int page, int pageSize, String keyword, String region);

    CustomerDetailResponse getCustomer(String id);
>>>>>>> origin/main
}
