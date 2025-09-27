package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

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
}
