package com.bob.mta.modules.customer.persistence;

import com.bob.mta.modules.customer.repository.CustomerRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnBean(CustomerMapper.class)
public class PersistenceCustomerRepository implements CustomerRepository {

    private final CustomerMapper mapper;

    public PersistenceCustomerRepository(CustomerMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<CustomerSummaryRecord> search(String tenantId, String keyword, String region, int offset, int limit) {
        return mapper.search(tenantId, keyword, region, offset, limit);
    }

    @Override
    public long count(String tenantId, String keyword, String region) {
        return mapper.count(tenantId, keyword, region);
    }

    @Override
    public CustomerDetailRecord findDetail(String tenantId, String customerId) {
        return mapper.findDetail(tenantId, customerId);
    }

    @Override
    public CustomerEntity findById(String tenantId, String customerId) {
        return mapper.findById(tenantId, customerId);
    }
}
