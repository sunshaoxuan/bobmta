package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
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
                .from(query.getFrom())
                .to(query.getTo())
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

        return new PlanAnalytics(filtered.size(), design, scheduled, inProgress, completed, canceled, overdue, upcoming);
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
}
