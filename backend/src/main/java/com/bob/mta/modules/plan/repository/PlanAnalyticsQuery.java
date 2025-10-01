package com.bob.mta.modules.plan.repository;

import java.time.OffsetDateTime;

public final class PlanAnalyticsQuery {

    private final String tenantId;
    private final OffsetDateTime from;
    private final OffsetDateTime to;
    private final String ownerId;
    private final OffsetDateTime referenceTime;
    private final int upcomingLimit;
    private final String customerId;
    private final int ownerLimit;
    private final int riskLimit;
    private final int dueSoonMinutes;

    private PlanAnalyticsQuery(String tenantId, String customerId, String ownerId,
                               OffsetDateTime from, OffsetDateTime to,
                               OffsetDateTime referenceTime, int upcomingLimit,
                               int ownerLimit, int riskLimit, int dueSoonMinutes) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.ownerId = ownerId;
        this.from = from;
        this.to = to;
        this.referenceTime = referenceTime;
        this.upcomingLimit = upcomingLimit;
        this.ownerLimit = ownerLimit;
        this.riskLimit = riskLimit;
        this.dueSoonMinutes = dueSoonMinutes;
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

    public String getOwnerId() {
        return ownerId;
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

    public int getOwnerLimit() {
        return ownerLimit;
    }

    public int getRiskLimit() {
        return riskLimit;
    }

    public int getDueSoonMinutes() {
        return dueSoonMinutes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String tenantId;
        private String customerId;
        private OffsetDateTime from;
        private OffsetDateTime to;
        private String ownerId;
        private OffsetDateTime referenceTime;
        private Integer upcomingLimit;
        private Integer ownerLimit;
        private Integer riskLimit;
        private Integer dueSoonMinutes;

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

        public Builder ownerId(String ownerId) {
            this.ownerId = ownerId;
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

        public Builder ownerLimit(Integer ownerLimit) {
            this.ownerLimit = ownerLimit;
            return this;
        }

        public Builder riskLimit(Integer riskLimit) {
            this.riskLimit = riskLimit;
            return this;
        }

        public Builder dueSoonMinutes(Integer dueSoonMinutes) {
            this.dueSoonMinutes = dueSoonMinutes;
            return this;
        }

        public PlanAnalyticsQuery build() {
            OffsetDateTime reference = referenceTime == null ? OffsetDateTime.now() : referenceTime;
            int limit = upcomingLimit == null || upcomingLimit <= 0 ? 5 : upcomingLimit;
            int owner = ownerLimit == null || ownerLimit <= 0 ? 5 : ownerLimit;
            int risk = riskLimit == null || riskLimit <= 0 ? 5 : riskLimit;
            int dueSoon = dueSoonMinutes == null || dueSoonMinutes <= 0 ? 1440 : dueSoonMinutes;
            return new PlanAnalyticsQuery(tenantId, customerId, ownerId, from, to, reference, limit, owner, risk, dueSoon);
        }
    }
}
