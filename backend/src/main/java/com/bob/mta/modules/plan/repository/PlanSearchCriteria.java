package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;

public final class PlanSearchCriteria {

    private final String tenantId;
    private final String customerId;
    private final String owner;
    private final String keyword;
    private final PlanStatus status;
    private final OffsetDateTime from;
    private final OffsetDateTime to;

    private PlanSearchCriteria(Builder builder) {
        this.tenantId = builder.tenantId;
        this.customerId = builder.customerId;
        this.owner = builder.owner;
        this.keyword = builder.keyword;
        this.status = builder.status;
        this.from = builder.from;
        this.to = builder.to;
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

    public OffsetDateTime getFrom() {
        return from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public static final class Builder {

        private String tenantId;
        private String customerId;
        private String owner;
        private String keyword;
        private PlanStatus status;
        private OffsetDateTime from;
        private OffsetDateTime to;

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

        public Builder from(OffsetDateTime from) {
            this.from = from;
            return this;
        }

        public Builder to(OffsetDateTime to) {
            this.to = to;
            return this;
        }

        public PlanSearchCriteria build() {
            return new PlanSearchCriteria(this);
        }
    }
}
