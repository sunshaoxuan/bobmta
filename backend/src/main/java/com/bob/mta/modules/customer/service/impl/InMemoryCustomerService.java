package com.bob.mta.modules.customer.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.customer.domain.Customer;
import com.bob.mta.modules.customer.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class InMemoryCustomerService implements CustomerService {

    private final ConcurrentMap<String, Customer> customers = new ConcurrentHashMap<>();

    public InMemoryCustomerService() {
        seedCustomers();
    }

    private void seedCustomers() {
        addCustomer(new Customer(
                "cust-001",
                "东京医疗中心",
                "东京",
                "医疗",
                List.of("重点客户", "长期合作"),
                Map.of("primary", "+81-3-0000-0000", "email", "contact@tokyo-med.jp"),
                Map.of("ERP版本", "R12", "核心系统", "患者管理平台"),
                OffsetDateTime.now().minusDays(2)
        ));
        addCustomer(new Customer(
                "cust-002",
                "大阪制造",
                "大阪",
                "制造",
                List.of("华东区", "生产"),
                Map.of("primary", "+81-6-0000-0000", "email", "ops@osaka-mfg.jp"),
                Map.of("工厂数量", "3", "关键联系人", "田中"),
                OffsetDateTime.now().minusDays(5)
        ));
    }

    private void addCustomer(Customer customer) {
        customers.put(customer.getId(), customer);
    }

    @Override
    public List<Customer> search(String keyword, String region) {
        return customers.values().stream()
                .filter(customer -> !StringUtils.hasText(keyword) || customer.getName().contains(keyword))
                .filter(customer -> !StringUtils.hasText(region) || region.equals(customer.getRegion()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public Customer getById(String id) {
        Customer customer = customers.get(id);
        if (customer == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return customer;
    }
}
