package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PlanBoardQuery {

    private final String tenantId;
    private final List<String> customerIds;
    private final String ownerId;
    private final List<PlanStatus> statuses;
    private final OffsetDateTime from;
    private final OffsetDateTime to;
    private final TimeGranularity granularity;

    private PlanBoardQuery(Builder builder) {
        this.tenantId = builder.tenantId;
        this.customerIds = builder.customerIds == null
                ? List.of()
                : List.copyOf(builder.customerIds);
        this.ownerId = builder.ownerId;
        this.statuses = builder.statuses == null
                ? List.of()
                : List.copyOf(builder.statuses);
        this.from = builder.from;
        this.to = builder.to;
        this.granularity = builder.granularity == null ? TimeGranularity.WEEK : builder.granularity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTenantId() {
        return tenantId;
    }

    public List<String> getCustomerIds() {
        return Collections.unmodifiableList(customerIds);
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<PlanStatus> getStatuses() {
        return Collections.unmodifiableList(statuses);
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public TimeGranularity getGranularity() {
        return granularity;
    }

    public boolean hasCustomerFilter() {
        return !customerIds.isEmpty();
    }

    public PlanSearchCriteria toCriteria() {
        PlanSearchCriteria.Builder builder = PlanSearchCriteria.builder()
                .tenantId(tenantId)
                .owner(ownerId)
                .from(from)
                .to(to)
                .statuses(statuses);

        if (customerIds.size() == 1) {
            builder.customerId(customerIds.get(0));
        }
        return builder.build();
    }

    public enum TimeGranularity {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    public static final class Builder {

        private String tenantId;
        private List<String> customerIds;
        private String ownerId;
        private List<PlanStatus> statuses;
        private OffsetDateTime from;
        private OffsetDateTime to;
        private TimeGranularity granularity;

        private Builder() {
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder customerIds(List<String> customerIds) {
            if (customerIds == null) {
                this.customerIds = null;
            } else {
                this.customerIds = new ArrayList<>();
                for (String customerId : customerIds) {
                    if (customerId != null && !customerId.isBlank()) {
                        this.customerIds.add(customerId);
                    }
                }
            }
            return this;
        }

        public Builder ownerId(String ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder statuses(List<PlanStatus> statuses) {
            if (statuses == null) {
                this.statuses = null;
            } else {
                this.statuses = statuses.stream()
                        .filter(Objects::nonNull)
                        .toList();
            }
            return this;
        }

        public Builder from(OffsetDateTime from) {
            this.from = from;
            return this;
        }

        public Builder to(OffsetDateTime to) {
            this.to = to;
            return this;
        }

        public Builder granularity(TimeGranularity granularity) {
            this.granularity = granularity;
            return this;
        }

        public PlanBoardQuery build() {
            return new PlanBoardQuery(this);
        }
    }
}

