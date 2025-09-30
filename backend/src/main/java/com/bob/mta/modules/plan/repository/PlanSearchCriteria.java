package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;
import java.util.List;

public final class PlanSearchCriteria {

    private final String tenantId;
    private final String customerId;
    private final String owner;
    private final String keyword;
    private final PlanStatus status;
    private final List<PlanStatus> statuses;
    private final OffsetDateTime from;
    private final OffsetDateTime to;
    private final Integer limit;
    private final Integer offset;
    private final String excludePlanId;

    private PlanSearchCriteria(Builder builder) {
        this.tenantId = builder.tenantId;
        this.customerId = builder.customerId;
        this.owner = builder.owner;
        this.keyword = builder.keyword;
        this.status = builder.status;
        this.statuses = builder.statuses != null
                ? builder.statuses
                : (builder.status == null ? List.of() : List.of(builder.status));
        this.from = builder.from;
        this.to = builder.to;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.excludePlanId = builder.excludePlanId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getOwner() {
        return owner;
    }

    public String getKeyword() {
        return keyword;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public List<PlanStatus> getStatuses() {
        return statuses;
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public String getExcludePlanId() {
        return excludePlanId;
    }

    public static final class Builder {

        private String tenantId;
        private String customerId;
        private String owner;
        private String keyword;
        private PlanStatus status;
        private List<PlanStatus> statuses;
        private OffsetDateTime from;
        private OffsetDateTime to;
        private Integer limit;
        private Integer offset;
        private String excludePlanId;

        private Builder() {
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder status(PlanStatus status) {
            this.status = status;
            return this;
        }

        public Builder statuses(List<PlanStatus> statuses) {
            this.statuses = statuses == null ? null : List.copyOf(statuses);
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

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public Builder excludePlanId(String excludePlanId) {
            this.excludePlanId = excludePlanId;
            return this;
        }

        public PlanSearchCriteria build() {
            return new PlanSearchCriteria(this);
        }
    }
}
