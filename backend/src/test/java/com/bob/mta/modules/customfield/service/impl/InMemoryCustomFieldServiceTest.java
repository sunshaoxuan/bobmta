package com.bob.mta.modules.customfield.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.customfield.domain.CustomFieldType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryCustomFieldServiceTest {

    private final InMemoryCustomFieldService service = new InMemoryCustomFieldService();

    @Test
    void shouldCreateDefinition() {
        var definition = service.createDefinition("priority", "Priority", CustomFieldType.TEXT, false, null, "Customer Priority");
        assertThat(service.getDefinition(definition.getId()).getLabel()).isEqualTo("Priority");
    }

    @Test
    void shouldValidateRequiredField() {
        var field = service.createDefinition("region", "Region", CustomFieldType.TEXT, true, null, null);
        service.updateValues("cust", Map.of(field.getId(), "Tokyo"));
        assertThat(service.listValues("cust")).hasSize(1);
    }

    @Test
    void shouldRejectInvalidNumber() {
        var numberField = service.createDefinition("sla", "SLA Hours", CustomFieldType.NUMBER, false, null, null);
        assertThatThrownBy(() -> service.updateValues("cust", Map.of(numberField.getId(), "abc")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CUSTOM_FIELD_VALUE_INVALID);
    }

    @Test
    void shouldFailWhenDefinitionMissing() {
        assertThatThrownBy(() -> service.getDefinition(9_999))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CUSTOM_FIELD_NOT_FOUND);
    }
}
