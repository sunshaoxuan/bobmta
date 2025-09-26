package com.bob.mta.modules.customer.dto;

<<<<<<< HEAD
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
=======
import java.time.Instant;
import java.util.List;

/**
 * Projection for listing customers in summary views.
 */
public class CustomerSummaryResponse {

    private final String id;

    private final String code;

    private final String name;

    private final String group;

    private final String region;

    private final List<String> tags;

    private final Instant updatedAt;

    public CustomerSummaryResponse(
            final String id,
            final String code,
            final String name,
            final String group,
            final String region,
            final List<String> tags,
            final Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.group = group;
        this.region = region;
        this.tags = List.copyOf(tags);
        this.updatedAt = updatedAt;
>>>>>>> origin/main
    }

    public String getId() {
        return id;
    }

<<<<<<< HEAD
=======
    public String getCode() {
        return code;
    }

>>>>>>> origin/main
    public String getName() {
        return name;
    }

<<<<<<< HEAD
=======
    public String getGroup() {
        return group;
    }

>>>>>>> origin/main
    public String getRegion() {
        return region;
    }

<<<<<<< HEAD
    public String getIndustry() {
        return industry;
    }

=======
>>>>>>> origin/main
    public List<String> getTags() {
        return tags;
    }

<<<<<<< HEAD
    public OffsetDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
=======
    public Instant getUpdatedAt() {
        return updatedAt;
>>>>>>> origin/main
    }
}
