package com.bob.mta.modules.customer.dto;

import com.bob.mta.modules.customer.domain.Customer;

import java.time.OffsetDateTime;
import java.util.List;

public class CustomerSummaryResponse {

    private final String id;
    private final String name;
    private final String region;
    private final String industry;
    private final List<String> tags;
    private final OffsetDateTime lastUpdatedAt;

    public CustomerSummaryResponse(String id, String name, String region, String industry,
                                   List<String> tags, OffsetDateTime lastUpdatedAt) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.industry = industry;
        this.tags = tags;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public static CustomerSummaryResponse from(Customer customer) {
        return new CustomerSummaryResponse(
                customer.getId(),
                customer.getName(),
                customer.getRegion(),
                customer.getIndustry(),
                customer.getTags(),
                customer.getLastUpdatedAt()
        );
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public String getIndustry() {
        return industry;
    }

    public List<String> getTags() {
        return tags;
    }

    public OffsetDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
