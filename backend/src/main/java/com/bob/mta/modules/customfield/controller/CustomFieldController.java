package com.bob.mta.modules.customfield.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.customfield.domain.CustomFieldDefinition;
import com.bob.mta.modules.customfield.dto.CreateCustomFieldRequest;
import com.bob.mta.modules.customfield.dto.CustomFieldDefinitionResponse;
import com.bob.mta.modules.customfield.dto.CustomFieldValueRequest;
import com.bob.mta.modules.customfield.dto.CustomFieldValueResponse;
import com.bob.mta.modules.customfield.dto.UpdateCustomFieldRequest;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.customfield.service.CustomFieldService;
import com.bob.mta.modules.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/custom-fields")
public class CustomFieldController {

    private final CustomFieldService customFieldService;
    private final CustomerService customerService;
    private final AuditRecorder auditRecorder;

    public CustomFieldController(CustomFieldService customFieldService, CustomerService customerService,
                                 AuditRecorder auditRecorder) {
        this.customFieldService = customFieldService;
        this.customerService = customerService;
        this.auditRecorder = auditRecorder;
    }

    @GetMapping
    public ApiResponse<List<CustomFieldDefinitionResponse>> listDefinitions() {
        List<CustomFieldDefinitionResponse> responses = customFieldService.listDefinitions().stream()
                .map(CustomFieldDefinitionResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CustomFieldDefinitionResponse> createDefinition(
            @Valid @RequestBody CreateCustomFieldRequest request) {
        CustomFieldDefinition definition = customFieldService.createDefinition(
                request.getCode(),
                request.getLabel(),
                request.getType(),
                request.isRequired(),
                request.getOptions(),
                request.getDescription());
        auditRecorder.record("CustomField", String.valueOf(definition.getId()), "CREATE_CUSTOM_FIELD",
                Localization.text(LocalizationKeys.Audit.CUSTOM_FIELD_CREATE),
                null, CustomFieldDefinitionResponse.from(definition));
        return ApiResponse.success(CustomFieldDefinitionResponse.from(definition));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CustomFieldDefinitionResponse> updateDefinition(@PathVariable long id,
                                                                       @Valid @RequestBody UpdateCustomFieldRequest request) {
        CustomFieldDefinition before = customFieldService.getDefinition(id);
        CustomFieldDefinition updated = customFieldService.updateDefinition(
                id,
                request.getLabel(),
                request.getType(),
                request.isRequired(),
                request.getOptions(),
                request.getDescription());
        auditRecorder.record("CustomField", String.valueOf(id), "UPDATE_CUSTOM_FIELD",
                Localization.text(LocalizationKeys.Audit.CUSTOM_FIELD_UPDATE),
                CustomFieldDefinitionResponse.from(before), CustomFieldDefinitionResponse.from(updated));
        return ApiResponse.success(CustomFieldDefinitionResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteDefinition(@PathVariable long id) {
        CustomFieldDefinition before = customFieldService.getDefinition(id);
        customFieldService.deleteDefinition(id);
        auditRecorder.record("CustomField", String.valueOf(id), "DELETE_CUSTOM_FIELD",
                Localization.text(LocalizationKeys.Audit.CUSTOM_FIELD_DELETE),
                CustomFieldDefinitionResponse.from(before), null);
        return ApiResponse.success();
    }

    @GetMapping("/customers/{customerId}")
    public ApiResponse<List<CustomFieldValueResponse>> getCustomerValues(@PathVariable String customerId) {
        customerService.getCustomer(customerId);
        List<CustomFieldValueResponse> responses = customFieldService.listValues(customerId).stream()
                .map(CustomFieldValueResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PutMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<List<CustomFieldValueResponse>> updateCustomerValues(@PathVariable String customerId,
                                                                            @RequestBody @Valid List<CustomFieldValueRequest> requests) {
        customerService.getCustomer(customerId);
        Map<Long, String> values = requests.stream()
                .collect(Collectors.toMap(CustomFieldValueRequest::getFieldId, CustomFieldValueRequest::getValue));
        List<CustomFieldValueResponse> updated = customFieldService.updateValues(customerId, values).stream()
                .map(CustomFieldValueResponse::from)
                .toList();
        auditRecorder.record("CustomFieldValue", customerId, "UPSERT_CUSTOM_FIELD_VALUE",
                Localization.text(LocalizationKeys.Audit.CUSTOM_FIELD_VALUE_UPSERT), null, updated);
        return ApiResponse.success(updated);
    }
}
