package com.bob.mta.modules.plan.service;

import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.Locale;

public final class PlanBoardViewHelper {

    private PlanBoardViewHelper() {
    }

    public static final Comparator<PlanBoardView.PlanCard> PLAN_CARD_COMPARATOR = Comparator
            .comparing(PlanBoardView.PlanCard::getPlannedStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PlanBoardView.PlanCard::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    public static OffsetDateTime normalizeBucketStart(OffsetDateTime time, PlanBoardGrouping grouping) {
        if (time == null) {
            return null;
        }
        return switch (grouping) {
            case DAY -> time.truncatedTo(ChronoUnit.DAYS);
            case WEEK -> time.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .truncatedTo(ChronoUnit.DAYS);
            case MONTH -> time.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case YEAR -> time.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
        };
    }

    public static OffsetDateTime normalizeBucketEnd(OffsetDateTime start, PlanBoardGrouping grouping) {
        if (start == null) {
            return null;
        }
        return switch (grouping) {
            case DAY -> start.plusDays(1);
            case WEEK -> start.plusWeeks(1);
            case MONTH -> start.plusMonths(1);
            case YEAR -> start.plusYears(1);
        };
    }

    public static String formatBucketLabel(OffsetDateTime start, PlanBoardGrouping grouping) {
        if (start == null) {
            return null;
        }
        return switch (grouping) {
            case DAY -> start.toLocalDate().toString();
            case WEEK -> {
                int week = start.get(WeekFields.ISO.weekOfWeekBasedYear());
                yield start.getYear() + "-W" + String.format(Locale.ROOT, "%02d", week);
            }
            case MONTH -> String.format(Locale.ROOT, "%d-%02d", start.getYear(), start.getMonthValue());
            case YEAR -> Integer.toString(start.getYear());
        };
    }

    public static boolean isActiveStatus(PlanStatus status) {
        return status == PlanStatus.SCHEDULED || status == PlanStatus.IN_PROGRESS;
    }

    public static Double durationHours(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        double minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            return 0.0;
        }
        return minutes / 60.0;
    }

    public static double roundAverage(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
