package com.bob.mta.modules.customer.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.customer.domain.Customer;
import com.bob.mta.modules.customer.service.CustomerService;
import com.bob.mta.modules.customfield.domain.CustomFieldValue;
import com.bob.mta.modules.customfield.service.CustomFieldService;
import com.bob.mta.modules.tag.domain.TagDefinition;
import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.service.TagService;
import com.bob.mta.modules.tag.domain.TagScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class InMemoryCustomerService implements CustomerService {

    private final TagService tagService;
    private final CustomFieldService customFieldService;
    private final ConcurrentMap<String, CustomerRecord> customers = new ConcurrentHashMap<>();

    public InMemoryCustomerService(TagService tagService, CustomFieldService customFieldService) {
        this.tagService = tagService;
        this.customFieldService = customFieldService;
        seedCustomers();
    }

    private void seedCustomers() {
        customers.put("cust-001", new CustomerRecord(
                "cust-001",
                "东京医疗中心",
                "东京",
                "医疗",
                Map.of("primary", "+81-3-0000-0000", "email", "contact@tokyo-med.jp"),
                OffsetDateTime.now().minusDays(2)
        ));
        customers.put("cust-002", new CustomerRecord(
                "cust-002",
                "大阪制造",
                "大阪",
                "制造",
                Map.of("primary", "+81-6-0000-0000", "email", "ops@osaka-mfg.jp"),
                OffsetDateTime.now().minusDays(5)
        ));

        long keyAccountTag = ensureTag("重点客户", "#FF4D4F", "StarOutlined");
        long longTermTag = ensureTag("长期合作", "#13C2C2", "TeamOutlined");
        tagService.assign(keyAccountTag, TagEntityType.CUSTOMER, "cust-001");
        tagService.assign(longTermTag, TagEntityType.CUSTOMER, "cust-001");

        long eastTag = ensureTag("华东区", "#722ED1", "GlobalOutlined");
        long productionTag = ensureTag("生产", "#EB2F96", "ToolOutlined");
        tagService.assign(eastTag, TagEntityType.CUSTOMER, "cust-002");
        tagService.assign(productionTag, TagEntityType.CUSTOMER, "cust-002");

        customFieldService.listDefinitions().forEach(def -> {
            if ("erp_version".equals(def.getCode())) {
                customFieldService.updateValues("cust-001", Map.of(def.getId(), "R12"));
                customFieldService.updateValues("cust-002", Map.of(def.getId(), "R11"));
            }
            if ("critical_system".equals(def.getCode())) {
                customFieldService.updateValues("cust-001", Map.of(def.getId(), "患者管理平台"));
                customFieldService.updateValues("cust-002", Map.of(def.getId(), "生产制造平台"));
            }
        });
    }

    private long ensureTag(String name, String color, String icon) {
        return tagService.list(null).stream()
                .filter(tag -> tag.getName().equals(name))
                .findFirst()
                .orElseGet(() -> tagService.create(name, color, icon, TagScope.CUSTOMER, null, true))
                .getId();
    }

    @Override
    public List<Customer> search(String keyword, String region) {
        return customers.values().stream()
                .map(this::toCustomer)
                .filter(customer -> !StringUtils.hasText(keyword) || customer.getName().contains(keyword))
                .filter(customer -> !StringUtils.hasText(region) || region.equals(customer.getRegion()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public Customer getById(String id) {
        CustomerRecord customer = customers.get(id);
        if (customer == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return toCustomer(customer);
    }

    private Customer toCustomer(CustomerRecord record) {
        List<String> tags = tagService.findByEntity(TagEntityType.CUSTOMER, record.id()).stream()
                .map(TagDefinition::getName)
                .toList();
        Map<String, String> customFields = customFieldService.listValues(record.id()).stream()
                .collect(Collectors.toMap(
                        value -> customFieldService.getDefinition(value.getFieldId()).getLabel(),
                        CustomFieldValue::getValue,
                        (a, b) -> b
                ));
        return new Customer(
                record.id(),
                record.name(),
                record.region(),
                record.industry(),
                tags,
                record.contacts(),
                customFields,
                record.lastUpdatedAt()
        );
    }

    private record CustomerRecord(String id, String name, String region, String industry,
                                  Map<String, String> contacts, OffsetDateTime lastUpdatedAt) {
    }
}
