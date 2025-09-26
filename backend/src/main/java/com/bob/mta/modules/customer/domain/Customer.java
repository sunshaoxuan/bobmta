package com.bob.mta.modules.customer.domain;

<<<<<<< HEAD
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
=======
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
    public String getShortName() {
        return shortName;
    }

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
    public Map<String, String> getContacts() {
        return contacts;
    }

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public OffsetDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
=======
    public Map<String, Object> getFields() {
        return fields;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
>>>>>>> origin/main
    }
}
