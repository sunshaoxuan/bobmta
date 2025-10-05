package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanRiskEvaluator;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.persistence.PlanAggregateMapper;
import com.bob.mta.modules.plan.persistence.PlanAnalyticsQueryParameters;
import com.bob.mta.modules.plan.persistence.PlanBoardCustomerAggregateEntity;
import com.bob.mta.modules.plan.persistence.PlanBoardPlanEntity;
import com.bob.mta.modules.plan.persistence.PlanBoardQueryParameters;
import com.bob.mta.modules.plan.persistence.PlanBoardTimeBucketEntity;
import com.bob.mta.modules.plan.persistence.PlanOwnerLoadEntity;
import com.bob.mta.modules.plan.persistence.PlanRiskPlanEntity;
import com.bob.mta.modules.plan.persistence.PlanStatusCountEntity;
import com.bob.mta.modules.plan.persistence.PlanUpcomingPlanEntity;
import com.bob.mta.modules.plan.service.PlanBoardView;
import com.bob.mta.modules.plan.service.PlanBoardViewHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@ConditionalOnBean(PlanAggregateMapper.class)
public class PlanPersistenceAnalyticsRepository implements PlanAnalyticsRepository {

    private final PlanAggregateMapper mapper;

    public PlanPersistenceAnalyticsRepository(PlanAggregateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PlanAnalytics summarize(PlanAnalyticsQuery query) {
        PlanAnalyticsQueryParameters parameters = PlanAnalyticsQueryParameters.fromQuery(query);

        List<PlanStatusCountEntity> statusCounts = mapper.countPlansByStatus(parameters);
        long overdue = mapper.countOverduePlans(parameters);
        List<PlanUpcomingPlanEntity> upcomingPlans = mapper.findUpcomingPlans(parameters);
        List<PlanOwnerLoadEntity> ownerLoads = mapper.findOwnerLoads(parameters);
        List<PlanRiskPlanEntity> riskPlans = mapper.findRiskPlans(parameters);

        Map<PlanStatus, Long> countsByStatus = new EnumMap<>(PlanStatus.class);
        for (PlanStatusCountEntity entry : statusCounts) {
            countsByStatus.put(entry.status(), entry.total());
        }

        long total = statusCounts.stream().mapToLong(PlanStatusCountEntity::total).sum();
        long design = countsByStatus.getOrDefault(PlanStatus.DESIGN, 0L);
        long scheduled = countsByStatus.getOrDefault(PlanStatus.SCHEDULED, 0L);
        long inProgress = countsByStatus.getOrDefault(PlanStatus.IN_PROGRESS, 0L);
        long completed = countsByStatus.getOrDefault(PlanStatus.COMPLETED, 0L);
        long canceled = countsByStatus.getOrDefault(PlanStatus.CANCELED, 0L);

        List<PlanAnalytics.UpcomingPlan> upcoming = upcomingPlans.stream()
                .map(entity -> new PlanAnalytics.UpcomingPlan(
                        entity.planId(),
                        entity.title(),
                        entity.status(),
                        entity.plannedStartTime(),
                        entity.plannedEndTime(),
                        entity.ownerId(),
                        entity.customerId(),
                        calculateProgress(entity.completedNodes(), entity.totalNodes())
                ))
                .toList();

        List<PlanAnalytics.OwnerLoad> ownerLoadAnalytics = ownerLoads.stream()
                .map(load -> new PlanAnalytics.OwnerLoad(
                        load.ownerId(),
                        load.totalPlans(),
                        load.activePlans(),
                        load.overduePlans()
                ))
                .toList();

        List<PlanAnalytics.RiskPlan> riskPlanAnalytics = riskPlans.stream()
                .filter(entity -> entity.riskLevel() != null)
                .map(entity -> new PlanAnalytics.RiskPlan(
                        entity.planId(),
                        entity.title(),
                        entity.status(),
                        entity.plannedEndTime(),
                        entity.ownerId(),
                        entity.customerId(),
                        toRiskLevel(entity.riskLevel()),
                        entity.minutesUntilDue() == null ? 0 : entity.minutesUntilDue(),
                        entity.minutesOverdue() == null ? 0 : entity.minutesOverdue()
                ))
                .toList();

        return new PlanAnalytics(total, design, scheduled, inProgress, completed, canceled, overdue, upcoming,
                ownerLoadAnalytics, riskPlanAnalytics);
    }

    @Override
    public PlanBoardView getPlanBoard(PlanSearchCriteria criteria, PlanBoardGrouping grouping) {
        PlanSearchCriteria effectiveCriteria = criteria == null ? PlanSearchCriteria.builder().build() : criteria;
        PlanBoardGrouping effectiveGrouping = grouping == null ? PlanBoardGrouping.WEEK : grouping;
        OffsetDateTime reference = OffsetDateTime.now();
        int dueSoonMinutes = PlanRiskEvaluator.DEFAULT_DUE_SOON_MINUTES;
        PlanBoardQueryParameters parameters = PlanBoardQueryParameters.fromCriteria(effectiveCriteria,
                effectiveGrouping, reference, dueSoonMinutes);

        List<PlanBoardPlanEntity> planEntities = mapper.findPlansForBoard(parameters);
        Map<String, List<PlanBoardView.PlanCard>> plansByCustomer = new HashMap<>();
        Map<String, List<PlanBoardView.PlanCard>> plansByBucket = new HashMap<>();

        for (PlanBoardPlanEntity entity : planEntities) {
            PlanBoardView.PlanCard card = toPlanCard(entity);
            String customerKey = normalizeCustomerId(entity.customerId());
            plansByCustomer.computeIfAbsent(customerKey, key -> new java.util.ArrayList<>()).add(card);

            if (entity.plannedStartTime() != null) {
                OffsetDateTime bucketStart = PlanBoardViewHelper.normalizeBucketStart(entity.plannedStartTime(),
                        effectiveGrouping);
                String bucketId = PlanBoardViewHelper.formatBucketLabel(bucketStart, effectiveGrouping);
                if (bucketId != null) {
                    plansByBucket.computeIfAbsent(bucketId, key -> new java.util.ArrayList<>()).add(card);
                }
            }
        }

        plansByCustomer.values().forEach(list -> list.sort(PlanBoardViewHelper.PLAN_CARD_COMPARATOR));
        plansByBucket.values().forEach(list -> list.sort(PlanBoardViewHelper.PLAN_CARD_COMPARATOR));

        List<PlanBoardCustomerAggregateEntity> customerAggregates = mapper.aggregateCustomers(parameters);
        List<PlanBoardView.CustomerGroup> customerGroups = customerAggregates.stream()
                .sorted((left, right) -> {
                    int compare = Long.compare(right.totalPlans(), left.totalPlans());
                    if (compare != 0) {
                        return compare;
                    }
                    return normalizeCustomerId(left.customerId()).compareTo(normalizeCustomerId(right.customerId()));
                })
                .map(entity -> {
                    String normalizedCustomerId = normalizeCustomerId(entity.customerId());
                    List<PlanBoardView.PlanCard> customerPlans = plansByCustomer.getOrDefault(normalizedCustomerId, List.of());
                    return new PlanBoardView.CustomerGroup(
                            normalizedCustomerId,
                            entity.customerName(),
                            entity.totalPlans(),
                            entity.activePlans(),
                            entity.completedPlans(),
                            entity.overduePlans(),
                            entity.dueSoonPlans(),
                            entity.atRiskPlans(),
                            PlanBoardViewHelper.roundAverage(entity.averageProgress()),
                            entity.earliestStart(),
                            entity.latestEnd(),
                            customerPlans
                    );
                })
                .toList();

        List<PlanBoardTimeBucketEntity> bucketAggregates = mapper.aggregateTimeBuckets(parameters);
        List<PlanBoardView.TimeBucket> timeBuckets = bucketAggregates.stream()
                .sorted((left, right) -> {
                    if (left.start() == null && right.start() == null) {
                        return 0;
                    }
                    if (left.start() == null) {
                        return 1;
                    }
                    if (right.start() == null) {
                        return -1;
                    }
                    return left.start().compareTo(right.start());
                })
                .map(bucket -> new PlanBoardView.TimeBucket(
                        bucket.bucketId(),
                        bucket.start(),
                        bucket.end(),
                        bucket.totalPlans(),
                        bucket.activePlans(),
                        bucket.completedPlans(),
                        bucket.overduePlans(),
                        bucket.dueSoonPlans(),
                        bucket.atRiskPlans(),
                        plansByBucket.getOrDefault(bucket.bucketId(), List.of())
                ))
                .toList();

        PlanBoardView.Metrics metrics = computeMetrics(planEntities);
        return new PlanBoardView(customerGroups, timeBuckets, metrics, effectiveGrouping, reference);
    }

    private int calculateProgress(Long completedNodes, Long totalNodes) {
        long total = totalNodes == null ? 0 : totalNodes;
        long completed = completedNodes == null ? 0 : completedNodes;
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(completed * 100.0 / total);
    }

    private PlanAnalytics.RiskLevel toRiskLevel(String value) {
        if (value == null) {
            return null;
        }
        return PlanAnalytics.RiskLevel.valueOf(value);
    }

    private PlanBoardView.PlanCard toPlanCard(PlanBoardPlanEntity entity) {
        int progress = entity.progress() == null ? 0 : entity.progress();
        return new PlanBoardView.PlanCard(
                entity.planId(),
                entity.title(),
                entity.status(),
                entity.ownerId(),
                entity.customerId(),
                entity.plannedStartTime(),
                entity.plannedEndTime(),
                entity.timezone(),
                progress,
                entity.overdue(),
                entity.dueSoon(),
                entity.minutesUntilDue(),
                entity.minutesOverdue()
        );
    }

    private PlanBoardView.Metrics computeMetrics(List<PlanBoardPlanEntity> plans) {
        if (plans.isEmpty()) {
            return new PlanBoardView.Metrics(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        long total = plans.size();
        long active = plans.stream()
                .filter(plan -> PlanBoardViewHelper.isActiveStatus(plan.status()))
                .count();
        long completed = plans.stream()
                .filter(plan -> plan.status() == com.bob.mta.modules.plan.domain.PlanStatus.COMPLETED)
                .count();
        long overdue = plans.stream().filter(PlanBoardPlanEntity::overdue).count();
        long dueSoon = plans.stream().filter(PlanBoardPlanEntity::dueSoon).count();
        double avgProgress = PlanBoardViewHelper.roundAverage(plans.stream()
                .map(entity -> entity.progress() == null ? 0 : entity.progress())
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0));
        double avgDuration = PlanBoardViewHelper.roundAverage(plans.stream()
                .map(entity -> PlanBoardViewHelper.durationHours(entity.plannedStartTime(), entity.plannedEndTime()))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0));
        double completionRate = PlanBoardViewHelper.roundAverage(total == 0 ? 0 : (completed * 100.0) / total);
        long atRisk = overdue + dueSoon;
        return new PlanBoardView.Metrics(total, active, completed, overdue, dueSoon, atRisk,
                avgProgress, avgDuration, completionRate);
    }

    private String normalizeCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return PlanBoardView.UNKNOWN_CUSTOMER_ID;
        }
        return customerId;
    }
}
