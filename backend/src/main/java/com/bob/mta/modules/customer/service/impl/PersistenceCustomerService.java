package com.bob.mta.modules.customer.service.impl;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.tenant.TenantContext;
import com.bob.mta.modules.customfield.domain.CustomFieldType;
import com.bob.mta.modules.customfield.persistence.CustomFieldDefinitionEntity;
import com.bob.mta.modules.customfield.persistence.CustomFieldDefinitionMapper;
import com.bob.mta.modules.customfield.persistence.CustomFieldValueEntity;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;
import com.bob.mta.modules.customer.persistence.CustomerDetailRecord;
import com.bob.mta.modules.customer.persistence.CustomerMapper;
import com.bob.mta.modules.customer.persistence.CustomerSummaryRecord;
import com.bob.mta.modules.customer.service.CustomerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnBean(CustomerMapper.class)
public class PersistenceCustomerService implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CustomFieldDefinitionMapper customFieldMapper;
    private final TenantContext tenantContext;

    public PersistenceCustomerService(
            CustomerMapper customerMapper,
            CustomFieldDefinitionMapper customFieldMapper,
            TenantContext tenantContext) {
        this.customerMapper = customerMapper;
        this.customFieldMapper = customFieldMapper;
        this.tenantContext = tenantContext;
    }

    @Override
    public PageResponse<CustomerSummaryResponse> listCustomers(int page, int pageSize, String keyword, String region) {
        String tenantId = tenantContext.getCurrentTenantId();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safeSize;
        List<CustomerSummaryRecord> records = customerMapper.search(tenantId, keyword, region, offset, safeSize);
        long total = customerMapper.count(tenantId, keyword, region);
        List<CustomerSummaryResponse> summaries = records.stream()
                .map(this::toSummary)
                .toList();
        return PageResponse.of(summaries, total, safePage, safeSize);
    }

    @Override
    public CustomerDetailResponse getCustomer(String id) {
        String tenantId = tenantContext.getCurrentTenantId();
        CustomerDetailRecord record = customerMapper.findDetail(tenantId, id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "customer.not_found");
        }
        Map<String, Object> fields = buildFieldMap(tenantId, id);
        return new CustomerDetailResponse(
                record.id(),
                record.code(),
                record.name(),
                record.shortName(),
                record.groupName(),
                record.region(),
                record.tags(),
                fields,
                record.updatedAt()
        );
    }

    private CustomerSummaryResponse toSummary(CustomerSummaryRecord record) {
        return new CustomerSummaryResponse(
                record.id(),
                record.code(),
                record.name(),
                record.groupName(),
                record.region(),
                record.tags(),
                record.updatedAt()
        );
    }

    private Map<String, Object> buildFieldMap(String tenantId, String entityId) {
        List<CustomFieldDefinitionEntity> definitions = customFieldMapper.list(tenantId);
        Map<Long, CustomFieldDefinitionEntity> definitionIndex = definitions.stream()
                .collect(Collectors.toMap(CustomFieldDefinitionEntity::getId, def -> def));
        List<CustomFieldValueEntity> values = customFieldMapper.listValues(tenantId, entityId);
        Map<String, Object> fields = new LinkedHashMap<>();
        for (CustomFieldValueEntity value : values) {
            CustomFieldDefinitionEntity definition = definitionIndex.get(value.fieldId());
            if (definition == null) {
                continue;
            }
            Object typedValue = convertValue(definition.getType(), value.value());
            if (typedValue != null) {
                fields.put(definition.getLabel(), typedValue);
            }
        }
        return fields;
    }

    private Object convertValue(CustomFieldType type, String raw) {
        if (raw == null) {
            return null;
        }
        return switch (type) {
            case NUMBER -> parseNumber(raw);
            case BOOLEAN -> Boolean.valueOf(raw);
            case DATE -> raw;
            case TEXT -> raw;
        };
    }

    private Double parseNumber(String raw) {
        try {
            return Double.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
