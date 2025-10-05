package com.bob.mta.modules.customer.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerMapper {

    List<CustomerSummaryRecord> search(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("region") String region,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long count(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword,
            @Param("region") String region);

    CustomerDetailRecord findDetail(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId);

    CustomerEntity findById(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId);
}
