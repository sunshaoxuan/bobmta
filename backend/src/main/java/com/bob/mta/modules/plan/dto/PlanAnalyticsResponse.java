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
    private final List<OwnerLoadResponse> ownerLoads;
    private final List<RiskPlanResponse> riskPlans;

    public PlanAnalyticsResponse(long totalPlans,
                                 long designCount,
                                 long scheduledCount,
                                 long inProgressCount,
                                 long completedCount,
                                 long canceledCount,
                                 long overdueCount,
                                 List<UpcomingPlanResponse> upcomingPlans,
                                 List<OwnerLoadResponse> ownerLoads,
                                 List<RiskPlanResponse> riskPlans) {
        this.totalPlans = totalPlans;
        this.designCount = designCount;
        this.scheduledCount = scheduledCount;
        this.inProgressCount = inProgressCount;
        this.completedCount = completedCount;
        this.canceledCount = canceledCount;
        this.overdueCount = overdueCount;
        this.upcomingPlans = upcomingPlans;
        this.ownerLoads = ownerLoads;
        this.riskPlans = riskPlans;
    }

    public static PlanAnalyticsResponse from(PlanAnalytics analytics) {
        List<UpcomingPlanResponse> upcoming = analytics.getUpcomingPlans().stream()
                .map(UpcomingPlanResponse::from)
                .toList();
        List<OwnerLoadResponse> ownerLoads = analytics.getOwnerLoads().stream()
                .map(OwnerLoadResponse::from)
                .toList();
        List<RiskPlanResponse> riskPlans = analytics.getRiskPlans().stream()
                .map(RiskPlanResponse::from)
                .toList();
        return new PlanAnalyticsResponse(
                analytics.getTotalPlans(),
                analytics.getDesignCount(),
                analytics.getScheduledCount(),
                analytics.getInProgressCount(),
                analytics.getCompletedCount(),
                analytics.getCanceledCount(),
                analytics.getOverdueCount(),
                upcoming,
                ownerLoads,
                riskPlans
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

    public List<OwnerLoadResponse> getOwnerLoads() {
        return ownerLoads;
    }

    public List<RiskPlanResponse> getRiskPlans() {
        return riskPlans;
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

    public static class OwnerLoadResponse {
        private final String ownerId;
        private final long totalPlans;
        private final long activePlans;
        private final long overduePlans;

        public OwnerLoadResponse(String ownerId, long totalPlans, long activePlans, long overduePlans) {
            this.ownerId = ownerId;
            this.totalPlans = totalPlans;
            this.activePlans = activePlans;
            this.overduePlans = overduePlans;
        }

        public static OwnerLoadResponse from(PlanAnalytics.OwnerLoad load) {
            return new OwnerLoadResponse(load.getOwnerId(), load.getTotalPlans(), load.getActivePlans(),
                    load.getOverduePlans());
        }

        public String getOwnerId() {
            return ownerId;
        }

        public long getTotalPlans() {
            return totalPlans;
        }

        public long getActivePlans() {
            return activePlans;
        }

        public long getOverduePlans() {
            return overduePlans;
        }
    }

    public static class RiskPlanResponse {
        private final String id;
        private final String title;
        private final PlanStatus status;
        private final OffsetDateTime plannedEndTime;
        private final String owner;
        private final String customerId;
        private final PlanAnalytics.RiskLevel riskLevel;
        private final long minutesUntilDue;
        private final long minutesOverdue;

        public RiskPlanResponse(String id,
                                String title,
                                PlanStatus status,
                                OffsetDateTime plannedEndTime,
                                String owner,
                                String customerId,
                                PlanAnalytics.RiskLevel riskLevel,
                                long minutesUntilDue,
                                long minutesOverdue) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.plannedEndTime = plannedEndTime;
            this.owner = owner;
            this.customerId = customerId;
            this.riskLevel = riskLevel;
            this.minutesUntilDue = minutesUntilDue;
            this.minutesOverdue = minutesOverdue;
        }

        public static RiskPlanResponse from(PlanAnalytics.RiskPlan plan) {
            return new RiskPlanResponse(
                    plan.getId(),
                    plan.getTitle(),
                    plan.getStatus(),
                    plan.getPlannedEndTime(),
                    plan.getOwner(),
                    plan.getCustomerId(),
                    plan.getRiskLevel(),
                    plan.getMinutesUntilDue(),
                    plan.getMinutesOverdue()
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

        public OffsetDateTime getPlannedEndTime() {
            return plannedEndTime;
        }

        public String getOwner() {
            return owner;
        }

        public String getCustomerId() {
            return customerId;
        }

        public PlanAnalytics.RiskLevel getRiskLevel() {
            return riskLevel;
        }

        public long getMinutesUntilDue() {
            return minutesUntilDue;
        }

        public long getMinutesOverdue() {
            return minutesOverdue;
        }
    }
}
