package com.bob.mta.modules.plan.repository;

import java.time.OffsetDateTime;

public final class PlanAnalyticsQuery {

    private final String tenantId;
    private final OffsetDateTime from;
    private final OffsetDateTime to;
    private final OffsetDateTime referenceTime;
    private final int upcomingLimit;
    private final String customerId;

    private PlanAnalyticsQuery(String tenantId, String customerId, OffsetDateTime from, OffsetDateTime to,
                               OffsetDateTime referenceTime, int upcomingLimit) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.from = from;
        this.to = to;
        this.referenceTime = referenceTime;
        this.upcomingLimit = upcomingLimit;
    }

    public String getTenantId() {
        return tenantId;
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public OffsetDateTime getReferenceTime() {
        return referenceTime;
    }

    public int getUpcomingLimit() {
        return upcomingLimit;
    }

    public String getCustomerId() {
        return customerId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String tenantId;
        private String customerId;
        private OffsetDateTime from;
        private OffsetDateTime to;
        private OffsetDateTime referenceTime;
        private Integer upcomingLimit;

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
        public Builder from(OffsetDateTime from) {
            this.from = from;
            return this;
        }

        public Builder to(OffsetDateTime to) {
            this.to = to;
            return this;
        }

        public Builder referenceTime(OffsetDateTime referenceTime) {
            this.referenceTime = referenceTime;
            return this;
        }

        public Builder upcomingLimit(Integer upcomingLimit) {
            this.upcomingLimit = upcomingLimit;
            return this;
        }

        public PlanAnalyticsQuery build() {
            OffsetDateTime reference = referenceTime == null ? OffsetDateTime.now() : referenceTime;
            int limit = upcomingLimit == null || upcomingLimit <= 0 ? 5 : upcomingLimit;
            return new PlanAnalyticsQuery(tenantId, customerId, from, to, reference, limit);
        }
    }
}
