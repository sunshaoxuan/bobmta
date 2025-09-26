package com.bob.mta.modules.customer.service;

import com.bob.mta.modules.customer.domain.Customer;

import java.util.List;

public interface CustomerService {

    List<Customer> search(String keyword, String region);

    Customer getById(String id);
}
