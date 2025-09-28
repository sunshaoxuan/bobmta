package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Locale;
import java.util.Objects;

@Repository
public class InMemoryPlanRepository implements PlanRepository {

    private final ConcurrentMap<String, Plan> storage = new ConcurrentHashMap<>();
    private final AtomicLong planSequence = new AtomicLong(5000);
    private final AtomicLong nodeSequence = new AtomicLong(1000);
    private final AtomicLong reminderSequence = new AtomicLong(9000);

    @Override
    public List<Plan> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Plan> findByCriteria(PlanSearchCriteria criteria) {
        if (criteria == null) {
            return findAll();
        }
        return storage.values().stream()
                .filter(plan -> criteria.getTenantId() == null
                        || Objects.equals(plan.getTenantId(), criteria.getTenantId()))
                .filter(plan -> criteria.getCustomerId() == null
                        || Objects.equals(plan.getCustomerId(), criteria.getCustomerId()))
                .filter(plan -> criteria.getOwner() == null
                        || Objects.equals(plan.getOwner(), criteria.getOwner()))
                .filter(plan -> matchesKeyword(plan, criteria.getKeyword()))
                .filter(plan -> criteria.getStatus() == null || plan.getStatus() == criteria.getStatus())
                .filter(plan -> criteria.getFrom() == null
                        || (plan.getPlannedEndTime() != null
                        && !plan.getPlannedEndTime().isBefore(criteria.getFrom())))
                .filter(plan -> criteria.getTo() == null
                        || (plan.getPlannedStartTime() != null
                        && !plan.getPlannedStartTime().isAfter(criteria.getTo())))
                .toList();
    }

    @Override
    public Optional<Plan> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public void save(Plan plan) {
        storage.put(plan.getId(), plan);
    }

    @Override
    public void delete(String id) {
        storage.remove(id);
    }

    @Override
    public String nextPlanId() {
        return "PLAN-" + planSequence.incrementAndGet();
    }

    @Override
    public String nextNodeId() {
        return "NODE-" + nodeSequence.incrementAndGet();
    }

    @Override
    public String nextReminderId() {
        return "REM-" + reminderSequence.incrementAndGet();
    }

    private boolean matchesKeyword(Plan plan, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(plan.getTitle(), keyword)
                || containsIgnoreCase(plan.getDescription(), keyword);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
}
