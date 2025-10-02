package com.bob.mta.modules.plan.domain;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Utility helpers for deriving risk signals for a plan based on its planned end time
 * and current execution status.
 */
public final class PlanRiskEvaluator {

    /**
     * Default window (in minutes) to treat active plans as "due soon" when they are
     * approaching the planned end time. Aligns with analytics dashboard defaults.
     */
    public static final int DEFAULT_DUE_SOON_MINUTES = 1440;

    private PlanRiskEvaluator() {
    }

    public static PlanRiskSnapshot evaluate(Plan plan, OffsetDateTime referenceTime) {
        return evaluate(plan, referenceTime, DEFAULT_DUE_SOON_MINUTES);
    }

    public static PlanRiskSnapshot evaluate(Plan plan, OffsetDateTime referenceTime, int dueSoonMinutes) {
        if (plan == null || referenceTime == null) {
            return PlanRiskSnapshot.inactive();
        }
        if (plan.getStatus() == PlanStatus.CANCELED
                || plan.getStatus() == PlanStatus.COMPLETED
                || plan.getStatus() == PlanStatus.DESIGN) {
            return PlanRiskSnapshot.inactive();
        }
        OffsetDateTime plannedEnd = plan.getPlannedEndTime();
        if (plannedEnd == null) {
            return PlanRiskSnapshot.inactive();
        }
        if (plannedEnd.isBefore(referenceTime)) {
            long overdueMinutes = Math.max(0, Duration.between(plannedEnd, referenceTime).toMinutes());
            return new PlanRiskSnapshot(true, false, null, overdueMinutes);
        }
        int window = dueSoonMinutes <= 0 ? DEFAULT_DUE_SOON_MINUTES : dueSoonMinutes;
        OffsetDateTime dueSoonThreshold = referenceTime.plusMinutes(window);
        if (!plannedEnd.isAfter(dueSoonThreshold)) {
            long minutesUntilDue = Math.max(0, Duration.between(referenceTime, plannedEnd).toMinutes());
            return new PlanRiskSnapshot(false, true, minutesUntilDue, null);
        }
        return PlanRiskSnapshot.inactive();
    }

    public record PlanRiskSnapshot(boolean overdue,
                                   boolean dueSoon,
                                   Long minutesUntilDue,
                                   Long minutesOverdue) {

        private static PlanRiskSnapshot inactive() {
            return new PlanRiskSnapshot(false, false, null, null);
        }
    }
}

