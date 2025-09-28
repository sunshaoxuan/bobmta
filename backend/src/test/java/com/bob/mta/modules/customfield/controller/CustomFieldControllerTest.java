package com.bob.mta.modules.customfield.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.customfield.domain.CustomFieldType;
import com.bob.mta.modules.customfield.dto.CreateCustomFieldRequest;
import com.bob.mta.modules.customfield.dto.CustomFieldDefinitionResponse;
import com.bob.mta.modules.customfield.dto.CustomFieldValueRequest;
import com.bob.mta.modules.customfield.service.impl.InMemoryCustomFieldService;
import com.bob.mta.modules.customer.service.impl.InMemoryCustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CustomFieldControllerTest {

    private CustomFieldController controller;
    private InMemoryCustomFieldService customFieldService;
    private MessageResolver messageResolver;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        customFieldService = new InMemoryCustomFieldService();
        InMemoryCustomerService customerService = new InMemoryCustomerService();
        AuditRecorder recorder = new AuditRecorder(new InMemoryAuditService(), new ObjectMapper());
        messageResolver = TestMessageResolverFactory.create();
        controller = new CustomFieldController(customFieldService, customerService, recorder, messageResolver);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "pass", "ROLE_ADMIN"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldCreateCustomField() {
        CreateCustomFieldRequest request = new CreateCustomFieldRequest();
        request.setCode("ticket_url");
        request.setLabel("Ticket URL");
        request.setType(CustomFieldType.TEXT);

        ApiResponse<CustomFieldDefinitionResponse> response = controller.createDefinition(request);
        assertThat(response.getData().getCode()).isEqualTo("ticket_url");
    }

    @Test
    void shouldUpdateCustomerValues() {
        var definition = customFieldService.createDefinition("priority", "Priority", CustomFieldType.TEXT, false, null, null);
        CustomFieldValueRequest valueRequest = new CustomFieldValueRequest();
        valueRequest.setFieldId(definition.getId());
        valueRequest.setValue("A");

        ApiResponse<?> response = controller.updateCustomerValues("cust-001", List.of(valueRequest));
        assertThat(response.getData()).isInstanceOf(List.class);
    }
}
