package com.bob.mta.modules.customer.repository;

import com.bob.mta.modules.customer.persistence.CustomerDetailRecord;
import com.bob.mta.modules.customer.persistence.CustomerEntity;
import com.bob.mta.modules.customer.persistence.CustomerSummaryRecord;

import java.util.List;

public interface CustomerRepository {

    List<CustomerSummaryRecord> search(String tenantId, String keyword, String region, int offset, int limit);

    long count(String tenantId, String keyword, String region);

    CustomerDetailRecord findDetail(String tenantId, String customerId);

    CustomerEntity findById(String tenantId, String customerId);
}
