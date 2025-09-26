package com.bob.mta.modules.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.modules.customer.domain.Customer;
import com.bob.mta.modules.customfield.service.impl.InMemoryCustomFieldService;
import com.bob.mta.modules.tag.service.impl.InMemoryTagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryCustomerServiceTest {

    private final InMemoryTagService tagService = new InMemoryTagService();
    private final InMemoryCustomFieldService customFieldService = new InMemoryCustomFieldService();
    private final InMemoryCustomerService service = new InMemoryCustomerService(tagService, customFieldService);

    @Test
    @DisplayName("keyword filter performs case-insensitive matching")
    void shouldFilterCustomersByKeyword() {
        final java.util.List<Customer> result = service.search("东京", "");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("东京");
    }

    @Test
    @DisplayName("missing customer id raises BusinessException")
    void shouldThrowWhenCustomerMissing() {
        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("customer detail returns immutable view of custom fields")
    void shouldExposeCustomerDetail() {
        final Customer detail = service.getById("cust-001");

        assertThat(detail.getId()).isEqualTo("cust-001");
        assertThat(detail.getCustomFields()).isNotEmpty();
        assertThat(detail.getTags()).contains("重点客户");
    }
}

