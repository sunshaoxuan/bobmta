package com.bob.mta.modules.customer.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;
import com.bob.mta.modules.customer.service.impl.InMemoryCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerControllerTest {

    private CustomerController controller;

    @BeforeEach
    void setUp() {
        controller = new CustomerController(new InMemoryCustomerService());
    }

    @Test
    @DisplayName("list endpoint delegates to service and returns page response")
    void shouldListCustomers() {
        final ApiResponse<PageResponse<CustomerSummaryResponse>> response = controller.list(1, 20, null, null);

        assertThat(response.getData().getList()).isNotEmpty();
    }

    @Test
    @DisplayName("detail endpoint returns customer information")
    void shouldReturnCustomerDetail() {
        final ApiResponse<CustomerDetailResponse> response = controller.detail("101");

        assertThat(response.getData().id()).isEqualTo("101");
    }
}
