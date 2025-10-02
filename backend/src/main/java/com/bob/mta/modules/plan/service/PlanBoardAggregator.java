package com.bob.mta.modules.plan.service;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanRiskEvaluator;
import com.bob.mta.modules.plan.domain.PlanRiskEvaluator.PlanRiskSnapshot;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PlanBoardAggregator {

    private static final Comparator<PlanBoardView.PlanCard> BOARD_PLAN_COMPARATOR = Comparator
            .comparing(PlanBoardView.PlanCard::getPlannedStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PlanBoardView.PlanCard::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private PlanBoardAggregator() {
    }

    public static PlanBoardView aggregate(List<Plan> plans, PlanBoardGrouping grouping) {
        OffsetDateTime reference = OffsetDateTime.now();
        return aggregate(plans, grouping, reference, PlanRiskEvaluator.DEFAULT_DUE_SOON_MINUTES);
    }

    public static PlanBoardView aggregate(List<Plan> plans, PlanBoardGrouping grouping,
                                       OffsetDateTime reference, int dueSoonMinutes) {
        List<Plan> safePlans = plans == null ? List.of() : List.copyOf(plans);
        PlanBoardGrouping effectiveGrouping = grouping == null ? PlanBoardGrouping.WEEK : grouping;

        PlanBoardView.Metrics metrics = computeMetrics(safePlans, reference, dueSoonMinutes);
        List<PlanBoardView.CustomerGroup> customerGroups = aggregateCustomers(safePlans, reference, dueSoonMinutes);
        List<PlanBoardView.TimeBucket> buckets = aggregateBuckets(safePlans, effectiveGrouping, reference, dueSoonMinutes);
        return new PlanBoardView(customerGroups, buckets, metrics, effectiveGrouping);
    }

    private static List<PlanBoardView.CustomerGroup> aggregateCustomers(List<Plan> plans,
                                                                        OffsetDateTime reference,
                                                                        int dueSoonMinutes) {
        if (plans.isEmpty()) {
            return List.of();
        }
        Map<String, List<Plan>> grouped = plans.stream()
                .collect(Collectors.groupingBy(plan -> plan.getCustomerId() == null
                        ? "UNKNOWN"
                        : plan.getCustomerId()));
        return grouped.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue().size(), left.getValue().size()))
                .map(entry -> toCustomerGroup(entry.getKey(), entry.getValue(), reference, dueSoonMinutes))
                .toList();
    }

    private static PlanBoardView.CustomerGroup toCustomerGroup(String customerId, List<Plan> plans,
                                                                OffsetDateTime reference, int dueSoonMinutes) {
        long total = plans.size();
        long active = plans.stream().filter(PlanBoardAggregator::isActivePlan).count();
        long completed = plans.stream().filter(plan -> plan.getStatus() == PlanStatus.COMPLETED).count();
        long overdue = plans.stream().map(plan -> PlanRiskEvaluator.evaluate(plan, reference, dueSoonMinutes))
                .filter(PlanRiskSnapshot::overdue)
                .count();
        long dueSoon = plans.stream().map(plan -> PlanRiskEvaluator.evaluate(plan, reference, dueSoonMinutes))
                .filter(PlanRiskSnapshot::dueSoon)
                .count();
        double avgProgress = roundAverage(plans.stream().mapToInt(Plan::getProgress).average().orElse(0));
        OffsetDateTime earliest = plans.stream()
                .map(Plan::getPlannedStartTime)
                .filter(Objects::nonNull)
                .min(OffsetDateTime::compareTo)
                .orElse(null);
        OffsetDateTime latest = plans.stream()
                .map(Plan::getPlannedEndTime)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
        List<PlanBoardView.PlanCard> cards = plans.stream()
                .map(plan -> toPlanCard(plan, reference, dueSoonMinutes))
                .sorted(BOARD_PLAN_COMPARATOR)
                .toList();
        return new PlanBoardView.CustomerGroup(customerId, null, total, active, completed,
                overdue, dueSoon, avgProgress, earliest, latest, cards);
    }

    private static List<PlanBoardView.TimeBucket> aggregateBuckets(List<Plan> plans,
                                                                   PlanBoardGrouping grouping,
                                                                   OffsetDateTime reference,
                                                                   int dueSoonMinutes) {
        if (plans.isEmpty()) {
            return List.of();
        }
        Map<OffsetDateTime, List<Plan>> bucketMap = new HashMap<>();
        for (Plan plan : plans) {
            OffsetDateTime plannedStart = plan.getPlannedStartTime();
            if (plannedStart == null) {
                continue;
            }
            OffsetDateTime bucketStart = normalizeBucketStart(plannedStart, grouping);
            bucketMap.computeIfAbsent(bucketStart, key -> new ArrayList<>()).add(plan);
        }
        return bucketMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toBucket(entry.getKey(), entry.getValue(), grouping, reference, dueSoonMinutes))
                .toList();
    }

    private static PlanBoardView.TimeBucket toBucket(OffsetDateTime bucketStart, List<Plan> plans,
                                                     PlanBoardGrouping grouping,
                                                     OffsetDateTime reference,
                                                     int dueSoonMinutes) {
        OffsetDateTime bucketEnd = normalizeBucketEnd(bucketStart, grouping);
        String bucketLabel = formatBucketLabel(bucketStart, grouping);
        long total = plans.size();
        long active = plans.stream().filter(PlanBoardAggregator::isActivePlan).count();
        long completed = plans.stream().filter(plan -> plan.getStatus() == PlanStatus.COMPLETED).count();
        List<PlanRiskSnapshot> risks = plans.stream()
                .map(plan -> PlanRiskEvaluator.evaluate(plan, reference, dueSoonMinutes))
                .toList();
        long overdue = risks.stream().filter(PlanRiskSnapshot::overdue).count();
        long dueSoon = risks.stream().filter(PlanRiskSnapshot::dueSoon).count();
        List<PlanBoardView.PlanCard> cards = plans.stream()
                .map(plan -> toPlanCard(plan, reference, dueSoonMinutes))
                .sorted(BOARD_PLAN_COMPARATOR)
                .toList();
        return new PlanBoardView.TimeBucket(bucketLabel, bucketStart, bucketEnd, total,
                active, completed, overdue, dueSoon, cards);
    }

    private static PlanBoardView.Metrics computeMetrics(List<Plan> plans,
                                                        OffsetDateTime reference,
                                                        int dueSoonMinutes) {
        if (plans.isEmpty()) {
            return new PlanBoardView.Metrics(0, 0, 0, 0, 0, 0, 0, 0);
        }
        long total = plans.size();
        long active = plans.stream().filter(PlanBoardAggregator::isActivePlan).count();
        long completed = plans.stream().filter(plan -> plan.getStatus() == PlanStatus.COMPLETED).count();
        List<PlanRiskSnapshot> risks = plans.stream()
                .map(plan -> PlanRiskEvaluator.evaluate(plan, reference, dueSoonMinutes))
                .toList();
        long overdue = risks.stream().filter(PlanRiskSnapshot::overdue).count();
        long dueSoon = risks.stream().filter(PlanRiskSnapshot::dueSoon).count();
        double avgProgress = roundAverage(plans.stream().mapToInt(Plan::getProgress).average().orElse(0));
        DoubleSummaryStatistics durations = plans.stream()
                .map(plan -> durationHours(plan.getPlannedStartTime(), plan.getPlannedEndTime()))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        double avgDuration = durations.getCount() == 0 ? 0 : roundAverage(durations.getAverage());
        double completionRate = total == 0 ? 0 : roundAverage((completed * 100.0) / total);
        return new PlanBoardView.Metrics(total, active, completed, overdue, dueSoon,
                avgProgress, avgDuration, completionRate);
    }

    private static PlanBoardView.PlanCard toPlanCard(Plan plan, OffsetDateTime reference, int dueSoonMinutes) {
        PlanRiskSnapshot risk = PlanRiskEvaluator.evaluate(plan, reference, dueSoonMinutes);
        return new PlanBoardView.PlanCard(
                plan.getId(),
                plan.getTitle(),
                plan.getStatus(),
                plan.getOwner(),
                plan.getCustomerId(),
                plan.getPlannedStartTime(),
                plan.getPlannedEndTime(),
                plan.getTimezone(),
                plan.getProgress(),
                risk.overdue(),
                risk.dueSoon(),
                risk.minutesUntilDue(),
                risk.minutesOverdue()
        );
    }

    private static OffsetDateTime normalizeBucketStart(OffsetDateTime time, PlanBoardGrouping grouping) {
        return switch (grouping) {
            case DAY -> time.truncatedTo(ChronoUnit.DAYS);
            case WEEK -> time.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
            case MONTH -> time.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case YEAR -> time.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
        };
    }

    private static OffsetDateTime normalizeBucketEnd(OffsetDateTime start, PlanBoardGrouping grouping) {
        return switch (grouping) {
            case DAY -> start.plusDays(1);
            case WEEK -> start.plusWeeks(1);
            case MONTH -> start.plusMonths(1);
            case YEAR -> start.plusYears(1);
        };
    }

    private static String formatBucketLabel(OffsetDateTime start, PlanBoardGrouping grouping) {
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

    private static boolean isActivePlan(Plan plan) {
        return plan.getStatus() == PlanStatus.SCHEDULED || plan.getStatus() == PlanStatus.IN_PROGRESS;
    }

    private static Double durationHours(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        double minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            return 0.0;
        }
        return minutes / 60.0;
    }

    private static double roundAverage(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}

