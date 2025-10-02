package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanRiskEvaluator;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.service.PlanBoardAggregator;
import com.bob.mta.modules.plan.service.PlanBoardView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@ConditionalOnMissingBean(com.bob.mta.modules.plan.persistence.PlanAggregateMapper.class)
public class InMemoryPlanAnalyticsRepository implements PlanAnalyticsRepository {

    private final PlanRepository planRepository;

    public InMemoryPlanAnalyticsRepository(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public PlanAnalytics summarize(PlanAnalyticsQuery query) {
        OffsetDateTime reference = query.getReferenceTime();

        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .tenantId(query.getTenantId())
                .customerId(query.getCustomerId())
                .owner(query.getOwnerId())
                .from(query.getFrom())
                .to(query.getTo())
                .statuses(query.getStatuses())
                .build();

        List<Plan> filtered = planRepository.findByCriteria(criteria).stream()
                .sorted(Comparator.comparing(Plan::getPlannedStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        long design = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.DESIGN).count();
        long scheduled = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.SCHEDULED).count();
        long inProgress = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.IN_PROGRESS).count();
        long completed = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.COMPLETED).count();
        long canceled = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.CANCELED).count();
        long overdue = filtered.stream().filter(plan -> isOverdue(plan, reference)).count();

        List<PlanAnalytics.UpcomingPlan> upcoming = filtered.stream()
                .filter(plan -> plan.getPlannedStartTime() != null)
                .filter(plan -> plan.getStatus() != PlanStatus.CANCELED && plan.getStatus() != PlanStatus.COMPLETED)
                .filter(plan -> !plan.getPlannedStartTime().isBefore(reference))
                .sorted(Comparator.comparing(Plan::getPlannedStartTime))
                .limit(query.getUpcomingLimit())
                .map(plan -> new PlanAnalytics.UpcomingPlan(
                        plan.getId(),
                        plan.getTitle(),
                        plan.getStatus(),
                        plan.getPlannedStartTime(),
                        plan.getPlannedEndTime(),
                        plan.getOwner(),
                        plan.getCustomerId(),
                        plan.getProgress()
                ))
                .collect(Collectors.toList());

        List<PlanAnalytics.OwnerLoad> ownerLoads = filtered.stream()
                .filter(plan -> plan.getOwner() != null && !plan.getOwner().isBlank())
                .collect(Collectors.groupingBy(Plan::getOwner))
                .entrySet().stream()
                .map(entry -> {
                    String owner = entry.getKey();
                    List<Plan> ownerPlans = entry.getValue();
                    long ownerTotal = ownerPlans.size();
                    long ownerActive = ownerPlans.stream()
                            .filter(plan -> plan.getStatus() == PlanStatus.SCHEDULED || plan.getStatus() == PlanStatus.IN_PROGRESS)
                            .count();
                    long ownerOverdue = ownerPlans.stream()
                            .filter(plan -> plan.getStatus() == PlanStatus.SCHEDULED || plan.getStatus() == PlanStatus.IN_PROGRESS)
                            .filter(plan -> isOverdue(plan, reference))
                            .count();
                    return new PlanAnalytics.OwnerLoad(owner, ownerTotal, ownerActive, ownerOverdue);
                })
                .sorted(Comparator.comparingLong(PlanAnalytics.OwnerLoad::getActivePlans).reversed()
                        .thenComparingLong(PlanAnalytics.OwnerLoad::getOverduePlans).reversed()
                        .thenComparing(PlanAnalytics.OwnerLoad::getOwnerId))
                .limit(query.getOwnerLimit())
                .toList();

        OffsetDateTime dueSoonThreshold = reference.plusMinutes(query.getDueSoonMinutes());
        List<PlanAnalytics.RiskPlan> riskPlans = filtered.stream()
                .filter(plan -> plan.getStatus() == PlanStatus.SCHEDULED || plan.getStatus() == PlanStatus.IN_PROGRESS)
                .filter(plan -> plan.getPlannedEndTime() != null)
                .map(plan -> toRiskPlan(plan, reference, dueSoonThreshold))
                .filter(risk -> risk != null)
                .sorted((a, b) -> {
                    if (a.getRiskLevel() != b.getRiskLevel()) {
                        return a.getRiskLevel() == PlanAnalytics.RiskLevel.OVERDUE ? -1 : 1;
                    }
                    if (a.getRiskLevel() == PlanAnalytics.RiskLevel.OVERDUE) {
                        return Long.compare(b.getMinutesOverdue(), a.getMinutesOverdue());
                    }
                    int compareDue = Long.compare(a.getMinutesUntilDue(), b.getMinutesUntilDue());
                    if (compareDue != 0) {
                        return compareDue;
                    }
                    return a.getId().compareTo(b.getId());
                })
                .limit(query.getRiskLimit())
                .toList();

        return new PlanAnalytics(filtered.size(), design, scheduled, inProgress, completed, canceled, overdue, upcoming,
                ownerLoads, riskPlans);
    }

    @Override
    public PlanBoardView getPlanBoard(String tenantId, PlanBoardWindow window, PlanBoardGrouping grouping) {
        PlanBoardWindow effectiveWindow = window == null ? PlanBoardWindow.builder().build() : window;
        PlanBoardGrouping effectiveGrouping = grouping == null ? PlanBoardGrouping.WEEK : grouping;
        var reference = OffsetDateTime.now();
        int dueSoonMinutes = PlanRiskEvaluator.DEFAULT_DUE_SOON_MINUTES;
        PlanSearchCriteria criteria = effectiveWindow.toCriteria(tenantId);
        List<Plan> candidates = planRepository.findByCriteria(criteria);
        if (effectiveWindow.hasCustomerFilter() && effectiveWindow.getCustomerIds().size() != 1) {
            Set<String> allowed = new LinkedHashSet<>(effectiveWindow.getCustomerIds());
            candidates = candidates.stream()
                    .filter(plan -> plan.getCustomerId() != null && allowed.contains(plan.getCustomerId()))
                    .toList();
        }
        return PlanBoardAggregator.aggregate(candidates, effectiveGrouping, reference, dueSoonMinutes);
    }

    private boolean isOverdue(Plan plan, OffsetDateTime reference) {
        if (plan.getStatus() == PlanStatus.CANCELED || plan.getStatus() == PlanStatus.COMPLETED) {
            return false;
        }
        if (plan.getStatus() == PlanStatus.DESIGN) {
            return false;
        }
        if (plan.getPlannedEndTime() == null) {
            return false;
        }
        return plan.getPlannedEndTime().isBefore(reference);
    }

    private PlanAnalytics.RiskPlan toRiskPlan(Plan plan, OffsetDateTime reference, OffsetDateTime dueSoonThreshold) {
        OffsetDateTime plannedEnd = plan.getPlannedEndTime();
        if (plannedEnd == null) {
            return null;
        }
        if (plannedEnd.isBefore(reference)) {
            long overdueMinutes = Math.max(0, reference.toEpochSecond() - plannedEnd.toEpochSecond()) / 60;
            return new PlanAnalytics.RiskPlan(
                    plan.getId(),
                    plan.getTitle(),
                    plan.getStatus(),
                    plannedEnd,
                    plan.getOwner(),
                    plan.getCustomerId(),
                    PlanAnalytics.RiskLevel.OVERDUE,
                    0,
                    overdueMinutes
            );
        }
        if (plannedEnd.isBefore(dueSoonThreshold)) {
            long minutesUntilDue = Math.max(0, plannedEnd.toEpochSecond() - reference.toEpochSecond()) / 60;
            return new PlanAnalytics.RiskPlan(
                    plan.getId(),
                    plan.getTitle(),
                    plan.getStatus(),
                    plannedEnd,
                    plan.getOwner(),
                    plan.getCustomerId(),
                    PlanAnalytics.RiskLevel.DUE_SOON,
                    minutesUntilDue,
                    0
            );
        }
        return null;
    }
}
