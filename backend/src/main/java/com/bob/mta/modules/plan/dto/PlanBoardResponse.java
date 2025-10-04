package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.service.PlanBoardView;

import java.time.OffsetDateTime;
import java.util.List;

public class PlanBoardResponse {

    private static final MetricsResponse ZERO_METRICS = new MetricsResponse(0, 0, 0, 0, 0, 0, 0, 0, 0);

    private final List<CustomerGroupResponse> customerGroups;
    private final List<TimeBucketResponse> timeBuckets;
    private final MetricsResponse metrics;
    private final String granularity;
    private final OffsetDateTime referenceTime;

    public PlanBoardResponse(List<CustomerGroupResponse> customerGroups,
                             List<TimeBucketResponse> timeBuckets,
                             MetricsResponse metrics,
                             String granularity,
                             OffsetDateTime referenceTime) {
        this.customerGroups = customerGroups == null ? List.of() : List.copyOf(customerGroups);
        this.timeBuckets = timeBuckets == null ? List.of() : List.copyOf(timeBuckets);
        this.metrics = metrics == null ? ZERO_METRICS : metrics;
        this.granularity = granularity;
        this.referenceTime = referenceTime;
    }

    public static PlanBoardResponse from(PlanBoardView view) {
        if (view == null) {
            return new PlanBoardResponse(List.of(), List.of(), ZERO_METRICS, null, null);
        }
        List<CustomerGroupResponse> groups = view.getCustomerGroups().stream()
                .map(CustomerGroupResponse::from)
                .toList();
        List<TimeBucketResponse> buckets = view.getTimeBuckets().stream()
                .map(TimeBucketResponse::from)
                .toList();
        MetricsResponse metricsResponse = view.getMetrics() == null
                ? ZERO_METRICS
                : MetricsResponse.from(view.getMetrics());
        String granularity = view.getGranularity() == null ? null : view.getGranularity().name();
        return new PlanBoardResponse(groups, buckets, metricsResponse, granularity, view.getReferenceTime());
    }

    public List<CustomerGroupResponse> getCustomerGroups() {
        return customerGroups;
    }

    public List<TimeBucketResponse> getTimeBuckets() {
        return timeBuckets;
    }

    public MetricsResponse getMetrics() {
        return metrics;
    }

    public String getGranularity() {
        return granularity;
    }

    public OffsetDateTime getReferenceTime() {
        return referenceTime;
    }

    public static class CustomerGroupResponse {
        private final String customerId;
        private final String customerName;
        private final long totalPlans;
        private final long activePlans;
        private final long completedPlans;
        private final long overduePlans;
        private final long dueSoonPlans;
        private final long atRiskPlans;
        private final double averageProgress;
        private final OffsetDateTime earliestStart;
        private final OffsetDateTime latestEnd;
        private final List<PlanCardResponse> plans;

        public CustomerGroupResponse(String customerId,
                                     String customerName,
                                     long totalPlans,
                                     long activePlans,
                                     long completedPlans,
                                     long overduePlans,
                                     long dueSoonPlans,
                                     long atRiskPlans,
                                     double averageProgress,
                                     OffsetDateTime earliestStart,
                                     OffsetDateTime latestEnd,
                                     List<PlanCardResponse> plans) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.totalPlans = totalPlans;
            this.activePlans = activePlans;
            this.completedPlans = completedPlans;
            this.overduePlans = overduePlans;
            this.dueSoonPlans = dueSoonPlans;
            this.atRiskPlans = atRiskPlans;
            this.averageProgress = averageProgress;
            this.earliestStart = earliestStart;
            this.latestEnd = latestEnd;
            this.plans = plans == null ? List.of() : List.copyOf(plans);
        }

        public static CustomerGroupResponse from(PlanBoardView.CustomerGroup group) {
            List<PlanCardResponse> plans = group.getPlans().stream()
                    .map(PlanCardResponse::from)
                    .toList();
            return new CustomerGroupResponse(
                    group.getCustomerId(),
                    group.getCustomerName(),
                    group.getTotalPlans(),
                    group.getActivePlans(),
                    group.getCompletedPlans(),
                    group.getOverduePlans(),
                    group.getDueSoonPlans(),
                    group.getAtRiskPlans(),
                    group.getAverageProgress(),
                    group.getEarliestStart(),
                    group.getLatestEnd(),
                    plans
            );
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

        public long getOverduePlans() {
            return overduePlans;
        }

        public long getDueSoonPlans() {
            return dueSoonPlans;
        }

        public long getAtRiskPlans() {
            return atRiskPlans;
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

        public List<PlanCardResponse> getPlans() {
            return plans;
        }
    }

    public static class TimeBucketResponse {
        private final String bucketId;
        private final OffsetDateTime start;
        private final OffsetDateTime end;
        private final long totalPlans;
        private final long activePlans;
        private final long completedPlans;
        private final long overduePlans;
        private final long dueSoonPlans;
        private final long atRiskPlans;
        private final List<PlanCardResponse> plans;

        public TimeBucketResponse(String bucketId,
                                  OffsetDateTime start,
                                  OffsetDateTime end,
                                  long totalPlans,
                                  long activePlans,
                                  long completedPlans,
                                  long overduePlans,
                                  long dueSoonPlans,
                                  long atRiskPlans,
                                  List<PlanCardResponse> plans) {
            this.bucketId = bucketId;
            this.start = start;
            this.end = end;
            this.totalPlans = totalPlans;
            this.activePlans = activePlans;
            this.completedPlans = completedPlans;
            this.overduePlans = overduePlans;
            this.dueSoonPlans = dueSoonPlans;
            this.atRiskPlans = atRiskPlans;
            this.plans = plans == null ? List.of() : List.copyOf(plans);
        }

        public static TimeBucketResponse from(PlanBoardView.TimeBucket bucket) {
            List<PlanCardResponse> plans = bucket.getPlans().stream()
                    .map(PlanCardResponse::from)
                    .toList();
            return new TimeBucketResponse(
                    bucket.getBucketId(),
                    bucket.getStart(),
                    bucket.getEnd(),
                    bucket.getTotalPlans(),
                    bucket.getActivePlans(),
                    bucket.getCompletedPlans(),
                    bucket.getOverduePlans(),
                    bucket.getDueSoonPlans(),
                    bucket.getAtRiskPlans(),
                    plans
            );
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

        public long getDueSoonPlans() {
            return dueSoonPlans;
        }

        public long getAtRiskPlans() {
            return atRiskPlans;
        }

        public List<PlanCardResponse> getPlans() {
            return plans;
        }
    }

    public static class PlanCardResponse {
        private final String id;
        private final String title;
        private final String status;
        private final String owner;
        private final String customerId;
        private final OffsetDateTime plannedStartTime;
        private final OffsetDateTime plannedEndTime;
        private final String timezone;
        private final int progress;
        private final boolean overdue;
        private final boolean dueSoon;
        private final Long minutesUntilDue;
        private final Long minutesOverdue;

        public PlanCardResponse(String id,
                                String title,
                                String status,
                                String owner,
                                String customerId,
                                OffsetDateTime plannedStartTime,
                                OffsetDateTime plannedEndTime,
                                String timezone,
                                int progress,
                                boolean overdue,
                                boolean dueSoon,
                                Long minutesUntilDue,
                                Long minutesOverdue) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.owner = owner;
            this.customerId = customerId;
            this.plannedStartTime = plannedStartTime;
            this.plannedEndTime = plannedEndTime;
            this.timezone = timezone;
            this.progress = progress;
            this.overdue = overdue;
            this.dueSoon = dueSoon;
            this.minutesUntilDue = minutesUntilDue;
            this.minutesOverdue = minutesOverdue;
        }

        public static PlanCardResponse from(PlanBoardView.PlanCard card) {
            return new PlanCardResponse(
                    card.getId(),
                    card.getTitle(),
                    card.getStatus() == null ? null : card.getStatus().name(),
                    card.getOwner(),
                    card.getCustomerId(),
                    card.getPlannedStartTime(),
                    card.getPlannedEndTime(),
                    card.getTimezone(),
                    card.getProgress(),
                    card.isOverdue(),
                    card.isDueSoon(),
                    card.getMinutesUntilDue(),
                    card.getMinutesOverdue()
            );
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getStatus() {
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

        public boolean isOverdue() {
            return overdue;
        }

        public boolean isDueSoon() {
            return dueSoon;
        }

        public Long getMinutesUntilDue() {
            return minutesUntilDue;
        }

        public Long getMinutesOverdue() {
            return minutesOverdue;
        }
    }

    public static class MetricsResponse {
        private final long totalPlans;
        private final long activePlans;
        private final long completedPlans;
        private final long overduePlans;
        private final long dueSoonPlans;
        private final long atRiskPlans;
        private final double averageProgress;
        private final double averageDurationHours;
        private final double completionRate;

        public MetricsResponse(long totalPlans,
                               long activePlans,
                               long completedPlans,
                               long overduePlans,
                               long dueSoonPlans,
                               long atRiskPlans,
                               double averageProgress,
                               double averageDurationHours,
                               double completionRate) {
            this.totalPlans = totalPlans;
            this.activePlans = activePlans;
            this.completedPlans = completedPlans;
            this.overduePlans = overduePlans;
            this.dueSoonPlans = dueSoonPlans;
            this.atRiskPlans = atRiskPlans;
            this.averageProgress = averageProgress;
            this.averageDurationHours = averageDurationHours;
            this.completionRate = completionRate;
        }

        public static MetricsResponse from(PlanBoardView.Metrics metrics) {
            return new MetricsResponse(
                    metrics.getTotalPlans(),
                    metrics.getActivePlans(),
                    metrics.getCompletedPlans(),
                    metrics.getOverduePlans(),
                    metrics.getDueSoonPlans(),
                    metrics.getAtRiskPlans(),
                    metrics.getAverageProgress(),
                    metrics.getAverageDurationHours(),
                    metrics.getCompletionRate()
            );
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

        public long getDueSoonPlans() {
            return dueSoonPlans;
        }

        public long getAtRiskPlans() {
            return atRiskPlans;
        }

        public double getAverageProgress() {
            return averageProgress;
        }

        public double getAverageDurationHours() {
            return averageDurationHours;
        }

        public double getCompletionRate() {
            return completionRate;
        }
    }
}

