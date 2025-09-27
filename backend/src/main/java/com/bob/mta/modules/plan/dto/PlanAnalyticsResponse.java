package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanStatus;

import java.time.OffsetDateTime;
import java.util.List;

public class PlanAnalyticsResponse {

    private final long totalPlans;
    private final long designCount;
    private final long scheduledCount;
    private final long inProgressCount;
    private final long completedCount;
    private final long canceledCount;
    private final long overdueCount;
    private final List<UpcomingPlanResponse> upcomingPlans;

    public PlanAnalyticsResponse(long totalPlans,
                                 long designCount,
                                 long scheduledCount,
                                 long inProgressCount,
                                 long completedCount,
                                 long canceledCount,
                                 long overdueCount,
                                 List<UpcomingPlanResponse> upcomingPlans) {
        this.totalPlans = totalPlans;
        this.designCount = designCount;
        this.scheduledCount = scheduledCount;
        this.inProgressCount = inProgressCount;
        this.completedCount = completedCount;
        this.canceledCount = canceledCount;
        this.overdueCount = overdueCount;
        this.upcomingPlans = upcomingPlans;
    }

    public static PlanAnalyticsResponse from(PlanAnalytics analytics) {
        List<UpcomingPlanResponse> upcoming = analytics.getUpcomingPlans().stream()
                .map(UpcomingPlanResponse::from)
                .toList();
        return new PlanAnalyticsResponse(
                analytics.getTotalPlans(),
                analytics.getDesignCount(),
                analytics.getScheduledCount(),
                analytics.getInProgressCount(),
                analytics.getCompletedCount(),
                analytics.getCanceledCount(),
                analytics.getOverdueCount(),
                upcoming
        );
    }

    public long getTotalPlans() {
        return totalPlans;
    }

    public long getDesignCount() {
        return designCount;
    }

    public long getScheduledCount() {
        return scheduledCount;
    }

    public long getInProgressCount() {
        return inProgressCount;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public long getCanceledCount() {
        return canceledCount;
    }

    public long getOverdueCount() {
        return overdueCount;
    }

    public List<UpcomingPlanResponse> getUpcomingPlans() {
        return upcomingPlans;
    }

    public static class UpcomingPlanResponse {
        private final String id;
        private final String title;
        private final PlanStatus status;
        private final OffsetDateTime plannedStartTime;
        private final OffsetDateTime plannedEndTime;
        private final String owner;
        private final String customerId;
        private final int progress;

        public UpcomingPlanResponse(String id,
                                    String title,
                                    PlanStatus status,
                                    OffsetDateTime plannedStartTime,
                                    OffsetDateTime plannedEndTime,
                                    String owner,
                                    String customerId,
                                    int progress) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.plannedStartTime = plannedStartTime;
            this.plannedEndTime = plannedEndTime;
            this.owner = owner;
            this.customerId = customerId;
            this.progress = progress;
        }

        public static UpcomingPlanResponse from(PlanAnalytics.UpcomingPlan plan) {
            return new UpcomingPlanResponse(
                    plan.getId(),
                    plan.getTitle(),
                    plan.getStatus(),
                    plan.getPlannedStartTime(),
                    plan.getPlannedEndTime(),
                    plan.getOwner(),
                    plan.getCustomerId(),
                    plan.getProgress()
            );
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

        public OffsetDateTime getPlannedStartTime() {
            return plannedStartTime;
        }

        public OffsetDateTime getPlannedEndTime() {
            return plannedEndTime;
        }

        public String getOwner() {
            return owner;
        }

        public String getCustomerId() {
            return customerId;
        }

        public int getProgress() {
            return progress;
        }
    }
}
