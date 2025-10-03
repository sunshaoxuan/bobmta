package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanActionHistory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@ConditionalOnMissingBean(PlanActionHistoryRepository.class)
public class InMemoryPlanActionHistoryRepository implements PlanActionHistoryRepository {

    private final ConcurrentMap<String, List<PlanActionHistory>> storage = new ConcurrentHashMap<>();

    @Override
    public void append(PlanActionHistory history) {
        Objects.requireNonNull(history, "history");
        storage.compute(history.getPlanId(), (planId, existing) -> {
            List<PlanActionHistory> next = existing == null
                    ? new ArrayList<>()
                    : new ArrayList<>(existing);
            next.add(history);
            next.sort((left, right) -> {
                int compare = left.getTriggeredAt().compareTo(right.getTriggeredAt());
                if (compare != 0) {
                    return compare;
                }
                return left.getId().compareTo(right.getId());
            });
            return next;
        });
    }

    @Override
    public List<PlanActionHistory> findByPlanId(String planId) {
        if (planId == null) {
            return List.of();
        }
        return List.copyOf(storage.getOrDefault(planId, List.of()));
    }

    @Override
    public void deleteByPlanId(String planId) {
        if (planId == null) {
            return;
        }
        storage.remove(planId);
    }
}
