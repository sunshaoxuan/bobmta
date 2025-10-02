package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.persistence.PlanAggregateMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@ConditionalOnMissingBean(PlanAggregateMapper.class)
public class InMemoryPlanRepository implements PlanAggregateRepository,
        PlanRepository,
        PlanReminderPolicyRepository,
        PlanTimelineRepository,
        PlanAttachmentRepository {

    @Override
    public PlanRepository plans() {
        return this;
    }

    @Override
    public PlanReminderPolicyRepository reminderPolicies() {
        return this;
    }

    @Override
    public PlanTimelineRepository timelines() {
        return this;
    }

    @Override
    public PlanAttachmentRepository attachments() {
        return this;
    }

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
        List<Plan> filtered = filter(criteria);
        if (criteria == null) {
            return filtered;
        }
        int offset = criteria.getOffset() == null ? 0 : Math.max(criteria.getOffset(), 0);
        Integer limit = criteria.getLimit() != null && criteria.getLimit() > 0 ? criteria.getLimit() : null;
        return filtered.stream()
                .skip(offset)
                .limit(limit == null ? Long.MAX_VALUE : limit)
                .collect(Collectors.toList());
    }

    @Override
    public int countByCriteria(PlanSearchCriteria criteria) {
        return filter(criteria).size();
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

    @Override
    public Optional<PlanReminderPolicy> findReminderPolicy(String planId) {
        return Optional.ofNullable(storage.get(planId))
                .map(Plan::getReminderPolicy);
    }

    @Override
    public void replaceReminderPolicy(String planId, PlanReminderPolicy policy) {
        if (policy == null) {
            return;
        }
        storage.computeIfPresent(planId, (id, current) -> rebuildPlan(current,
                current.getNodes(),
                current.getExecutions(),
                policy,
                current.getActivities(),
                policy.getUpdatedAt()));
    }

    @Override
    public List<PlanActivity> findTimeline(String planId) {
        Plan plan = storage.get(planId);
        return plan == null ? List.of() : plan.getActivities();
    }

    @Override
    public void replaceTimeline(String planId, List<PlanActivity> activities) {
        storage.computeIfPresent(planId, (id, current) -> rebuildPlan(current,
                current.getNodes(),
                current.getExecutions(),
                current.getReminderPolicy(),
                activities,
                current.getUpdatedAt()));
    }

    @Override
    public Map<String, List<String>> findAttachments(String planId) {
        Plan plan = storage.get(planId);
        if (plan == null) {
            return Map.of();
        }
        Map<String, List<String>> attachments = new LinkedHashMap<>();
        for (PlanNodeExecution execution : plan.getExecutions()) {
            attachments.put(execution.getNodeId(), new ArrayList<>(execution.getFileIds()));
        }
        return attachments;
    }

    @Override
    public void replaceAttachments(String planId, Map<String, List<String>> attachments) {
        if (attachments == null) {
            return;
        }
        storage.computeIfPresent(planId, (id, current) -> {
            Map<String, List<String>> normalized = new LinkedHashMap<>();
            attachments.forEach((nodeId, fileIds) -> {
                if (nodeId == null || fileIds == null) {
                    return;
                }
                normalized.put(nodeId, List.copyOf(fileIds));
            });
            List<PlanNodeExecution> updatedExecutions = current.getExecutions().stream()
                    .map(execution -> new PlanNodeExecution(
                            execution.getNodeId(),
                            execution.getStatus(),
                            execution.getStartTime(),
                            execution.getEndTime(),
                            execution.getOperator(),
                            execution.getResult(),
                            execution.getLog(),
                            normalized.getOrDefault(execution.getNodeId(), execution.getFileIds())
                    ))
                    .collect(Collectors.toList());
            return rebuildPlan(current,
                    current.getNodes(),
                    updatedExecutions,
                    current.getReminderPolicy(),
                    current.getActivities(),
                    current.getUpdatedAt());
        });
    }

    private Plan rebuildPlan(Plan original,
                             List<PlanNode> nodes,
                             List<PlanNodeExecution> executions,
                             PlanReminderPolicy reminderPolicy,
                             List<PlanActivity> activities,
                             OffsetDateTime updatedAt) {
        return new Plan(
                original.getId(),
                original.getTenantId(),
                original.getTitle(),
                original.getDescription(),
                original.getCustomerId(),
                original.getOwner(),
                original.getParticipants(),
                original.getStatus(),
                original.getPlannedStartTime(),
                original.getPlannedEndTime(),
                original.getActualStartTime(),
                original.getActualEndTime(),
                original.getCancelReason(),
                original.getCanceledBy(),
                original.getCanceledAt(),
                original.getTimezone(),
                nodes,
                executions,
                original.getCreatedAt(),
                updatedAt == null ? original.getUpdatedAt() : updatedAt,
                activities == null ? original.getActivities() : activities,
                reminderPolicy == null ? original.getReminderPolicy() : reminderPolicy
        );
    }

    private boolean matchesKeyword(Plan plan, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(plan.getTitle(), keyword)
                || containsIgnoreCase(plan.getDescription(), keyword);
    }

    private List<Plan> filter(PlanSearchCriteria criteria) {
        Comparator<Plan> comparator = Comparator
                .comparing(Plan::getPlannedStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Plan::getId, Comparator.nullsLast(Comparator.naturalOrder()));

        Set<String> allowedCustomers = criteria == null
                ? Set.of()
                : new LinkedHashSet<>(criteria.getCustomerIds());
        return storage.values().stream()
                .filter(plan -> criteria == null || criteria.getTenantId() == null
                        || Objects.equals(plan.getTenantId(), criteria.getTenantId()))
                .filter(plan -> {
                    if (criteria == null) {
                        return true;
                    }
                    if (!allowedCustomers.isEmpty()) {
                        return plan.getCustomerId() != null && allowedCustomers.contains(plan.getCustomerId());
                    }
                    return criteria.getCustomerId() == null
                            || Objects.equals(plan.getCustomerId(), criteria.getCustomerId());
                })
                .filter(plan -> criteria == null || criteria.getOwner() == null
                        || Objects.equals(plan.getOwner(), criteria.getOwner()))
                .filter(plan -> criteria == null || matchesKeyword(plan, criteria.getKeyword()))
                .filter(plan -> criteria == null || criteria.getStatus() == null
                        || plan.getStatus() == criteria.getStatus())
                .filter(plan -> criteria == null || criteria.getStatuses().isEmpty()
                        || criteria.getStatuses().contains(plan.getStatus()))
                .filter(plan -> criteria == null || criteria.getFrom() == null
                        || (plan.getPlannedEndTime() != null
                        && !plan.getPlannedEndTime().isBefore(criteria.getFrom())))
                .filter(plan -> criteria == null || criteria.getTo() == null
                        || (plan.getPlannedStartTime() != null
                        && !plan.getPlannedStartTime().isAfter(criteria.getTo())))
                .filter(plan -> criteria == null || criteria.getExcludePlanId() == null
                        || !Objects.equals(plan.getId(), criteria.getExcludePlanId()))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private boolean containsIgnoreCase(String value, String needle) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
}
