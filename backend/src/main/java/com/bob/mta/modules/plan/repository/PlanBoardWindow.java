package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PlanBoardWindow {

    private final OffsetDateTime from;
    private final OffsetDateTime to;
    private final String ownerId;
    private final List<String> customerIds;
    private final List<PlanStatus> statuses;

    private PlanBoardWindow(Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.ownerId = builder.ownerId;
        this.customerIds = builder.customerIds == null ? List.of() : List.copyOf(builder.customerIds);
        this.statuses = builder.statuses == null ? List.of() : List.copyOf(builder.statuses);
    }

    public static Builder builder() {
        return new Builder();
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<String> getCustomerIds() {
        return Collections.unmodifiableList(customerIds);
    }

    public List<PlanStatus> getStatuses() {
        return Collections.unmodifiableList(statuses);
    }

    public boolean hasCustomerFilter() {
        return !customerIds.isEmpty();
    }

    public PlanSearchCriteria toCriteria(String tenantId) {
        PlanSearchCriteria.Builder builder = PlanSearchCriteria.builder()
                .tenantId(tenantId)
                .owner(ownerId)
                .from(from)
                .to(to)
                .statuses(statuses.isEmpty() ? null : statuses);
        if (customerIds.size() == 1) {
            builder.customerId(customerIds.get(0));
        }
        return builder.build();
    }

    public static final class Builder {

        private OffsetDateTime from;
        private OffsetDateTime to;
        private String ownerId;
        private List<String> customerIds;
        private List<PlanStatus> statuses;

        private Builder() {
        }

        public Builder from(OffsetDateTime from) {
            this.from = from;
            return this;
        }

        public Builder to(OffsetDateTime to) {
            this.to = to;
            return this;
        }

        public Builder ownerId(String ownerId) {
            this.ownerId = ownerId;
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

        public PlanBoardWindow build() {
            return new PlanBoardWindow(this);
        }
    }
}

