package com.bob.mta.modules.plan.domain;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class PlanAnalytics {

    private final long totalPlans;
    private final long designCount;
    private final long scheduledCount;
    private final long inProgressCount;
    private final long completedCount;
    private final long canceledCount;
    private final long overdueCount;
    private final List<UpcomingPlan> upcomingPlans;
    private final List<OwnerLoad> ownerLoads;
    private final List<RiskPlan> riskPlans;

    public PlanAnalytics(long totalPlans,
                         long designCount,
                         long scheduledCount,
                         long inProgressCount,
                         long completedCount,
                         long canceledCount,
                         long overdueCount,
                         List<UpcomingPlan> upcomingPlans,
                         List<OwnerLoad> ownerLoads,
                         List<RiskPlan> riskPlans) {
        this.totalPlans = totalPlans;
        this.designCount = designCount;
        this.scheduledCount = scheduledCount;
        this.inProgressCount = inProgressCount;
        this.completedCount = completedCount;
        this.canceledCount = canceledCount;
        this.overdueCount = overdueCount;
        this.upcomingPlans = upcomingPlans == null ? List.of() : List.copyOf(upcomingPlans);
        this.ownerLoads = ownerLoads == null ? List.of() : List.copyOf(ownerLoads);
        this.riskPlans = riskPlans == null ? List.of() : List.copyOf(riskPlans);
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

    public List<UpcomingPlan> getUpcomingPlans() {
        return Collections.unmodifiableList(upcomingPlans);
    }

    public List<OwnerLoad> getOwnerLoads() {
        return Collections.unmodifiableList(ownerLoads);
    }

    public List<RiskPlan> getRiskPlans() {
        return Collections.unmodifiableList(riskPlans);
    }

    public static class UpcomingPlan {
        private final String id;
        private final String title;
        private final PlanStatus status;
        private final OffsetDateTime plannedStartTime;
        private final OffsetDateTime plannedEndTime;
        private final String owner;
        private final String customerId;
        private final int progress;

        public UpcomingPlan(String id,
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

    public static class OwnerLoad {
        private final String ownerId;
        private final long totalPlans;
        private final long activePlans;
        private final long overduePlans;

        public OwnerLoad(String ownerId, long totalPlans, long activePlans, long overduePlans) {
            this.ownerId = ownerId;
            this.totalPlans = totalPlans;
            this.activePlans = activePlans;
            this.overduePlans = overduePlans;
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

    public enum RiskLevel {
        OVERDUE,
        DUE_SOON
    }

    public static class RiskPlan {
        private final String id;
        private final String title;
        private final PlanStatus status;
        private final OffsetDateTime plannedEndTime;
        private final String owner;
        private final String customerId;
        private final RiskLevel riskLevel;
        private final long minutesUntilDue;
        private final long minutesOverdue;

        public RiskPlan(String id,
                        String title,
                        PlanStatus status,
                        OffsetDateTime plannedEndTime,
                        String owner,
                        String customerId,
                        RiskLevel riskLevel,
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

        public RiskLevel getRiskLevel() {
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
