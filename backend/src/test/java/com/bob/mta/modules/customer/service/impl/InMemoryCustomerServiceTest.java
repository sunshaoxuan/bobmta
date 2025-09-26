package com.bob.mta.modules.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryCustomerServiceTest {

    private final InMemoryCustomerService service = new InMemoryCustomerService();

    @Test
    @DisplayName("keyword filter performs case-insensitive matching")
    void shouldFilterCustomersByKeyword() {
        final PageResponse<CustomerSummaryResponse> response = service.listCustomers(1, 20, "tokyo", null);

        assertThat(response.getList()).hasSize(1);
        assertThat(response.getList().get(0).name()).contains("东京");
    }

    @Test
    @DisplayName("missing customer id raises BusinessException")
    void shouldThrowWhenCustomerMissing() {
        assertThatThrownBy(() -> service.getCustomer("missing"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("customer detail returns immutable view of custom fields")
    void shouldExposeCustomerDetail() {
        final CustomerDetailResponse detail = service.getCustomer("101");

        assertThat(detail.id()).isEqualTo("101");
        assertThat(detail.fields()).isNotEmpty();
        assertThat(detail.tags()).contains("重点客户");
    }
}

