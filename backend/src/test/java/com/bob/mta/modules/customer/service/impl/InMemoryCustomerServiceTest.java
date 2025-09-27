package com.bob.mta.modules.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

<<<<<<< HEAD
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.modules.customer.domain.Customer;
import com.bob.mta.modules.customfield.service.impl.InMemoryCustomFieldService;
import com.bob.mta.modules.tag.service.impl.InMemoryTagService;
=======
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;
>>>>>>> origin/main
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryCustomerServiceTest {

<<<<<<< HEAD
    private final InMemoryTagService tagService = new InMemoryTagService();
    private final InMemoryCustomFieldService customFieldService = new InMemoryCustomFieldService();
    private final InMemoryCustomerService service = new InMemoryCustomerService(tagService, customFieldService);
=======
    private final InMemoryCustomerService service = new InMemoryCustomerService();
>>>>>>> origin/main

    @Test
    @DisplayName("keyword filter performs case-insensitive matching")
    void shouldFilterCustomersByKeyword() {
<<<<<<< HEAD
        final java.util.List<Customer> result = service.search("东京", "");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("东京");
=======
        final PageResponse<CustomerSummaryResponse> response = service.listCustomers(1, 20, "tokyo", null);

        assertThat(response.getList()).hasSize(1);
        assertThat(response.getList().get(0).name()).contains("东京");
>>>>>>> origin/main
    }

    @Test
    @DisplayName("missing customer id raises BusinessException")
    void shouldThrowWhenCustomerMissing() {
<<<<<<< HEAD
        assertThatThrownBy(() -> service.getById("missing"))
=======
        assertThatThrownBy(() -> service.getCustomer("missing"))
>>>>>>> origin/main
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("customer detail returns immutable view of custom fields")
    void shouldExposeCustomerDetail() {
<<<<<<< HEAD
        final Customer detail = service.getById("cust-001");

        assertThat(detail.getId()).isEqualTo("cust-001");
        assertThat(detail.getCustomFields()).isNotEmpty();
        assertThat(detail.getTags()).contains("重点客户");
=======
        final CustomerDetailResponse detail = service.getCustomer("101");

        assertThat(detail.id()).isEqualTo("101");
        assertThat(detail.fields()).isNotEmpty();
        assertThat(detail.tags()).contains("重点客户");
>>>>>>> origin/main
    }
}

