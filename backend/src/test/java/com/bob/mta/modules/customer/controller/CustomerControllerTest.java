package com.bob.mta.modules.customer.controller;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;
import com.bob.mta.modules.customfield.service.impl.InMemoryCustomFieldService;
import com.bob.mta.modules.customer.service.impl.InMemoryCustomerService;
import com.bob.mta.modules.tag.service.impl.InMemoryTagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerControllerTest {

    private CustomerController controller;

    @BeforeEach
    void setUp() {
        InMemoryTagService tagService = new InMemoryTagService();
        InMemoryCustomFieldService customFieldService = new InMemoryCustomFieldService();
        controller = new CustomerController(new InMemoryCustomerService(tagService, customFieldService));
    }

    @Test
    void searchShouldReturnPagedResults() {
        PageResponse<CustomerSummaryResponse> page = controller.search("", "", 0, 1).getData();
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void detailShouldReturnCustomerInfo() {
        CustomerDetailResponse response = controller.detail("cust-001").getData();
        assertThat(response.getId()).isEqualTo("cust-001");
        assertThat(response.getCustomFields()).isNotEmpty();
    }
}
