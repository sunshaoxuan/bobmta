package com.bob.mta.modules.customer.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class Customer {

    private final String id;
    private final String name;
    private final String region;
    private final String industry;
    private final List<String> tags;
    private final Map<String, String> contacts;
    private final Map<String, String> customFields;
    private final OffsetDateTime lastUpdatedAt;

    public Customer(String id, String name, String region, String industry, List<String> tags,
                    Map<String, String> contacts, Map<String, String> customFields, OffsetDateTime lastUpdatedAt) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.industry = industry;
        this.tags = List.copyOf(tags);
        this.contacts = Map.copyOf(contacts);
        this.customFields = Map.copyOf(customFields);
        this.lastUpdatedAt = lastUpdatedAt;
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

    public Map<String, String> getContacts() {
        return contacts;
    }

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public OffsetDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
