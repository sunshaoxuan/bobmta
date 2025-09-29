package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.persistence.PlanAggregateMapper;
import com.bob.mta.modules.plan.persistence.PlanAnalyticsQueryParameters;
import com.bob.mta.modules.plan.persistence.PlanStatusCountEntity;
import com.bob.mta.modules.plan.persistence.PlanUpcomingPlanEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

        return new PlanAnalytics(total, design, scheduled, inProgress, completed, canceled, overdue, upcoming);
    }

    private int calculateProgress(Long completedNodes, Long totalNodes) {
        long total = totalNodes == null ? 0 : totalNodes;
        long completed = completedNodes == null ? 0 : completedNodes;
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(completed * 100.0 / total);
    }
}
