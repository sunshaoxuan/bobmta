package com.bob.mta.modules.customer.service.impl;

import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.customer.domain.Customer;
import com.bob.mta.modules.customer.persistence.CustomerMapper;
import com.bob.mta.modules.customer.dto.CustomerDetailResponse;
import com.bob.mta.modules.customer.dto.CustomerSummaryResponse;
import com.bob.mta.modules.customer.service.CustomerService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * In-memory implementation providing design-time data for API contracts.
 */
@Service
@ConditionalOnMissingBean(CustomerMapper.class)
public class InMemoryCustomerService implements CustomerService {

    private final List<Customer> customers;

    public InMemoryCustomerService() {
        customers = List.of(
                new Customer(
                        "101",
                        "CUST-101",
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_HOKKAIDO_NAME),
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_HOKKAIDO_ABBREVIATION),
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_HOKKAIDO_INDUSTRY),
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_HOKKAIDO_REGION),
                        List.of(
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_HOKKAIDO_TAG_PRIMARY),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_HOKKAIDO_TAG_SECONDARY)
                        ),
                        Map.of(
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_CONNECTIVITY_TYPE_LABEL),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_HOKKAIDO_CONNECTIVITY_VALUE),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_IP_ADDRESS_LABEL), "203.0.113.10",
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_TOOL_LABEL),
                                List.of("GlobalProtect", "RemoteView"),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_NOTE_LABEL),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_HOKKAIDO_NOTE)),
                        Instant.parse("2024-05-01T02:30:00Z")),
                new Customer(
                        "102",
                        "CUST-102",
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_KITAMI_NAME),
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_KITAMI_ABBREVIATION),
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_KITAMI_INDUSTRY),
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_KITAMI_REGION),
                        List.of(Localization.text(LocalizationKeys.Seeds.CUSTOMER_KITAMI_TAG_PRIMARY)),
                        Map.of(
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_CONNECTIVITY_TYPE_LABEL),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_KITAMI_CONNECTIVITY_VALUE),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_IP_ADDRESS_LABEL), "198.51.100.77",
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_TOOL_LABEL), List.of("LAPLINK"),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_NOTE_LABEL),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_KITAMI_NOTE)),
                        Instant.parse("2024-04-11T09:20:00Z")),
                new Customer(
                        "201",
                        "CUST-201",
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_TOKYO_METRO_NAME),
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_TOKYO_METRO_ABBREVIATION),
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_TOKYO_METRO_INDUSTRY),
                        Localization.text(LocalizationKeys.Seeds.CUSTOMER_TOKYO_METRO_REGION),
                        List.of(Localization.text(LocalizationKeys.Seeds.CUSTOMER_TOKYO_METRO_TAG_PRIMARY)),
                        Map.of(
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_CONNECTIVITY_TYPE_LABEL),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_TOKYO_METRO_CONNECTIVITY_VALUE),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_TOOL_LABEL), List.of("Cisco AnyConnect"),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_FIELD_NOTE_LABEL),
                                Localization.text(LocalizationKeys.Seeds.CUSTOMER_TOKYO_METRO_NOTE)),
                        Instant.parse("2024-04-25T12:00:00Z")));
    }

    @Override
    public PageResponse<CustomerSummaryResponse> listCustomers(
            final int page, final int pageSize, final String keyword, final String region) {
        final List<Customer> filtered = customers.stream()
                .filter(customer -> filterByKeyword(customer, keyword))
                .filter(customer -> filterByRegion(customer, region))
                .sorted(Comparator.comparing(Customer::getCode))
                .toList();
        final int fromIndex = Math.max(0, Math.min(filtered.size(), (page - 1) * pageSize));
        final int toIndex = Math.max(fromIndex, Math.min(filtered.size(), fromIndex + pageSize));
        final List<CustomerSummaryResponse> pageData = filtered.subList(fromIndex, toIndex).stream()
                .map(customer -> new CustomerSummaryResponse(
                        customer.getId(),
                        customer.getCode(),
                        customer.getName(),
                        customer.getGroup(),
                        customer.getRegion(),
                        customer.getTags(),
                        customer.getUpdatedAt()))
                .collect(Collectors.toList());
        return PageResponse.of(pageData, filtered.size(), page, pageSize);
    }

    @Override
    public CustomerDetailResponse getCustomer(final String id) {
        return customers.stream()
                .filter(customer -> customer.getId().equals(id))
                .findFirst()
                .map(customer -> new CustomerDetailResponse(
                        customer.getId(),
                        customer.getCode(),
                        customer.getName(),
                        customer.getShortName(),
                        customer.getGroup(),
                        customer.getRegion(),
                        customer.getTags(),
                        customer.getFields(),
                        customer.getUpdatedAt()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "customer.not_found"));
    }

    private boolean filterByKeyword(final Customer customer, final String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        final String normalized = keyword.toLowerCase(Locale.ROOT);
        return customer.getName().toLowerCase(Locale.ROOT).contains(normalized)
                || customer.getCode().toLowerCase(Locale.ROOT).contains(normalized)
                || customer.getShortName().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private boolean filterByRegion(final Customer customer, final String region) {
        if (!StringUtils.hasText(region)) {
            return true;
        }
        return region.equalsIgnoreCase(customer.getRegion());
    }
}