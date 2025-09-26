package com.bob.mta.modules.customer.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Aggregate root representing a tenant customer and its dynamic fields.
 */
public class Customer {

    private final String id;

    private final String code;

    private final String name;

    private final String shortName;

    private final String group;

    private final String region;

    private final List<String> tags;

    private final Map<String, Object> fields;

    private final Instant updatedAt;

    public Customer(
            final String id,
            final String code,
            final String name,
            final String shortName,
            final String group,
            final String region,
            final List<String> tags,
            final Map<String, Object> fields,
            final Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.shortName = shortName;
        this.group = group;
        this.region = region;
        this.tags = List.copyOf(tags);
        this.fields = Map.copyOf(fields);
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

    public String getShortName() {
        return shortName;
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

    public Map<String, Object> getFields() {
        return fields;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
