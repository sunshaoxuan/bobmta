package com.bob.mta.modules.customer.dto;

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
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getRegion() {
        return region;
    }

    public List<String> getTags() {
        return tags;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}