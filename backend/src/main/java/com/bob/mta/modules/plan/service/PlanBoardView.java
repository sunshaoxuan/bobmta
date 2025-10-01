package com.bob.mta.modules.plan.service;

import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanBoardQuery;

import java.time.OffsetDateTime;
import java.util.List;

public class PlanBoardView {

    private final List<CustomerGroup> customerGroups;
    private final List<TimeBucket> timeBuckets;
    private final Metrics metrics;
    private final PlanBoardQuery.TimeGranularity granularity;

    public PlanBoardView(List<CustomerGroup> customerGroups,
                         List<TimeBucket> timeBuckets,
                         Metrics metrics,
                         PlanBoardQuery.TimeGranularity granularity) {
        this.customerGroups = customerGroups == null ? List.of() : List.copyOf(customerGroups);
        this.timeBuckets = timeBuckets == null ? List.of() : List.copyOf(timeBuckets);
        this.metrics = metrics;
        this.granularity = granularity;
    }

    public List<CustomerGroup> getCustomerGroups() {
        return customerGroups;
    }

    public List<TimeBucket> getTimeBuckets() {
        return timeBuckets;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public PlanBoardQuery.TimeGranularity getGranularity() {
        return granularity;
    }

    public static class CustomerGroup {
        private final String customerId;
        private final String customerName;
        private final long totalPlans;
        private final long activePlans;
        private final long completedPlans;
        private final double averageProgress;
        private final OffsetDateTime earliestStart;
        private final OffsetDateTime latestEnd;
        private final List<PlanCard> plans;

        public CustomerGroup(String customerId,
                             String customerName,
                             long totalPlans,
                             long activePlans,
                             long completedPlans,
                             double averageProgress,
                             OffsetDateTime earliestStart,
                             OffsetDateTime latestEnd,
                             List<PlanCard> plans) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.totalPlans = totalPlans;
            this.activePlans = activePlans;
            this.completedPlans = completedPlans;
            this.averageProgress = averageProgress;
            this.earliestStart = earliestStart;
            this.latestEnd = latestEnd;
            this.plans = plans == null ? List.of() : List.copyOf(plans);
        }

        public String getCustomerId() {
            return customerId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public long getTotalPlans() {
            return totalPlans;
        }

        public long getActivePlans() {
            return activePlans;
        }

        public long getCompletedPlans() {
            return completedPlans;
        }

        public double getAverageProgress() {
            return averageProgress;
        }

        public OffsetDateTime getEarliestStart() {
            return earliestStart;
        }

        public OffsetDateTime getLatestEnd() {
            return latestEnd;
        }

        public List<PlanCard> getPlans() {
            return plans;
        }
    }

    public static class TimeBucket {
        private final String bucketId;
        private final OffsetDateTime start;
        private final OffsetDateTime end;
        private final long totalPlans;
        private final long activePlans;
        private final long completedPlans;
        private final long overduePlans;
        private final List<PlanCard> plans;

        public TimeBucket(String bucketId,
                          OffsetDateTime start,
                          OffsetDateTime end,
                          long totalPlans,
                          long activePlans,
                          long completedPlans,
                          long overduePlans,
                          List<PlanCard> plans) {
            this.bucketId = bucketId;
            this.start = start;
            this.end = end;
            this.totalPlans = totalPlans;
            this.activePlans = activePlans;
            this.completedPlans = completedPlans;
            this.overduePlans = overduePlans;
            this.plans = plans == null ? List.of() : List.copyOf(plans);
        }

        public String getBucketId() {
            return bucketId;
        }

        public OffsetDateTime getStart() {
            return start;
        }

        public OffsetDateTime getEnd() {
            return end;
        }

        public long getTotalPlans() {
            return totalPlans;
        }

        public long getActivePlans() {
            return activePlans;
        }

        public long getCompletedPlans() {
            return completedPlans;
        }

        public long getOverduePlans() {
            return overduePlans;
        }

        public List<PlanCard> getPlans() {
            return plans;
        }
    }

    public static class PlanCard {
        private final String id;
        private final String title;
        private final PlanStatus status;
        private final String owner;
        private final String customerId;
        private final OffsetDateTime plannedStartTime;
        private final OffsetDateTime plannedEndTime;
        private final String timezone;
        private final int progress;

        public PlanCard(String id,
                        String title,
                        PlanStatus status,
                        String owner,
                        String customerId,
                        OffsetDateTime plannedStartTime,
                        OffsetDateTime plannedEndTime,
                        String timezone,
                        int progress) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.owner = owner;
            this.customerId = customerId;
            this.plannedStartTime = plannedStartTime;
            this.plannedEndTime = plannedEndTime;
            this.timezone = timezone;
            this.progress = progress;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public PlanStatus getStatus() {
            return status;
        }

        public String getOwner() {
            return owner;
        }

        public String getCustomerId() {
            return customerId;
        }

        public OffsetDateTime getPlannedStartTime() {
            return plannedStartTime;
        }

        public OffsetDateTime getPlannedEndTime() {
            return plannedEndTime;
        }

        public String getTimezone() {
            return timezone;
        }

        public int getProgress() {
            return progress;
        }
    }

    public static class Metrics {
        private final long totalPlans;
        private final long activePlans;
        private final long completedPlans;
        private final long overduePlans;
        private final double averageProgress;
        private final double averageDurationHours;

        public Metrics(long totalPlans,
                       long activePlans,
                       long completedPlans,
                       long overduePlans,
                       double averageProgress,
                       double averageDurationHours) {
            this.totalPlans = totalPlans;
            this.activePlans = activePlans;
            this.completedPlans = completedPlans;
            this.overduePlans = overduePlans;
            this.averageProgress = averageProgress;
            this.averageDurationHours = averageDurationHours;
        }

        public long getTotalPlans() {
            return totalPlans;
        }

        public long getActivePlans() {
            return activePlans;
        }

        public long getCompletedPlans() {
            return completedPlans;
        }

        public long getOverduePlans() {
            return overduePlans;
        }

        public double getAverageProgress() {
            return averageProgress;
        }

        public double getAverageDurationHours() {
            return averageDurationHours;
        }
    }
}

