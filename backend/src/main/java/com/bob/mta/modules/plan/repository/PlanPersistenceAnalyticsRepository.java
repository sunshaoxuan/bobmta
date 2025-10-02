package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.persistence.PlanAggregateMapper;
import com.bob.mta.modules.plan.persistence.PlanAnalyticsQueryParameters;
import com.bob.mta.modules.plan.persistence.PlanOwnerLoadEntity;
import com.bob.mta.modules.plan.persistence.PlanRiskPlanEntity;
import com.bob.mta.modules.plan.persistence.PlanStatusCountEntity;
import com.bob.mta.modules.plan.persistence.PlanUpcomingPlanEntity;
import com.bob.mta.modules.plan.service.PlanBoardAggregator;
import com.bob.mta.modules.plan.service.PlanBoardView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@ConditionalOnBean(PlanAggregateMapper.class)
public class PlanPersistenceAnalyticsRepository implements PlanAnalyticsRepository {

    private final PlanAggregateMapper mapper;
    private final PlanRepository planRepository;

    public PlanPersistenceAnalyticsRepository(PlanAggregateMapper mapper, PlanRepository planRepository) {
        this.mapper = mapper;
        this.planRepository = planRepository;
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
    public PlanBoardView getPlanBoard(String tenantId, PlanBoardWindow window, PlanBoardGrouping grouping) {
        PlanBoardWindow effectiveWindow = window == null ? PlanBoardWindow.builder().build() : window;
        PlanBoardGrouping effectiveGrouping = grouping == null ? PlanBoardGrouping.WEEK : grouping;
        PlanSearchCriteria criteria = effectiveWindow.toCriteria(tenantId);
        List<Plan> candidates = planRepository.findByCriteria(criteria);
        if (effectiveWindow.hasCustomerFilter() && effectiveWindow.getCustomerIds().size() != 1) {
            Set<String> allowed = new LinkedHashSet<>(effectiveWindow.getCustomerIds());
            candidates = candidates.stream()
                    .filter(plan -> plan.getCustomerId() != null && allowed.contains(plan.getCustomerId()))
                    .toList();
        }
        return PlanBoardAggregator.aggregate(candidates, effectiveGrouping);
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
}
