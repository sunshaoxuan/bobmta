package com.bob.mta.modules.customfield.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.modules.customfield.domain.CustomFieldType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryCustomFieldServiceTest {

    private final InMemoryCustomFieldService service = new InMemoryCustomFieldService();

    @Test
    void shouldCreateDefinition() {
        var definition = service.createDefinition("priority", "优先级", CustomFieldType.TEXT, false, null, "客户优先级");
        assertThat(service.getDefinition(definition.getId()).getLabel()).isEqualTo("优先级");
    }

    @Test
    void shouldValidateRequiredField() {
        var field = service.createDefinition("region", "地区", CustomFieldType.TEXT, true, null, null);
        service.updateValues("cust", Map.of(field.getId(), "东京"));
        assertThat(service.listValues("cust")).hasSize(1);
    }

    @Test
    void shouldRejectInvalidNumber() {
        var numberField = service.createDefinition("sla", "SLA小时", CustomFieldType.NUMBER, false, null, null);
        assertThatThrownBy(() -> service.updateValues("cust", Map.of(numberField.getId(), "abc")))
                .isInstanceOf(BusinessException.class);
    }
}
