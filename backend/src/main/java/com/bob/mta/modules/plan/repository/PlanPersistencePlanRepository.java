package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.persistence.PlanActivityEntity;
import com.bob.mta.modules.plan.persistence.PlanAggregate;
import com.bob.mta.modules.plan.persistence.PlanAggregateMapper;
import com.bob.mta.modules.plan.persistence.PlanEntity;
import com.bob.mta.modules.plan.persistence.PlanNodeAttachmentEntity;
import com.bob.mta.modules.plan.persistence.PlanNodeEntity;
import com.bob.mta.modules.plan.persistence.PlanNodeExecutionEntity;
import com.bob.mta.modules.plan.persistence.PlanParticipantEntity;
import com.bob.mta.modules.plan.persistence.PlanPersistenceMapper;
import com.bob.mta.modules.plan.persistence.PlanQueryParameters;
import com.bob.mta.modules.plan.persistence.PlanReminderRuleEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@ConditionalOnBean(PlanAggregateMapper.class)
public class PlanPersistencePlanRepository implements PlanRepository {

    private final PlanAggregateMapper mapper;

    public PlanPersistencePlanRepository(PlanAggregateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Plan> findAll() {
        return toDomain(loadAggregates(mapper.findPlans(PlanQueryParameters.empty())));
    }

    @Override
    public List<Plan> findByCriteria(PlanSearchCriteria criteria) {
        PlanQueryParameters parameters = PlanQueryParameters.fromCriteria(criteria);
        return toDomain(loadAggregates(mapper.findPlans(parameters)));
    }

    @Override
    public int countByCriteria(PlanSearchCriteria criteria) {
        PlanQueryParameters parameters = PlanQueryParameters.fromCriteria(criteria);
        return mapper.countPlans(parameters);
    }

    @Override
    public Optional<Plan> findById(String id) {
        PlanEntity entity = mapper.findPlanById(id);
        if (entity == null) {
            return Optional.empty();
        }
        List<Plan> plans = toDomain(loadAggregates(List.of(entity)));
        return plans.isEmpty() ? Optional.empty() : Optional.of(plans.get(0));
    }

    @Override
    public void save(Plan plan) {
        Objects.requireNonNull(plan, "plan");
        PlanAggregate aggregate = PlanPersistenceMapper.toAggregate(plan);
        boolean exists = mapper.findPlanById(plan.getId()) != null;
        if (exists) {
            mapper.updatePlan(aggregate.plan());
            cleanupAssociations(plan.getId());
        } else {
            mapper.insertPlan(aggregate.plan());
        }
        persistAssociations(aggregate);
    }

    @Override
    public void delete(String id) {
        Objects.requireNonNull(id, "id");
        cleanupAssociations(id);
        mapper.deletePlan(id);
    }

    @Override
    public String nextPlanId() {
        return mapper.nextPlanId();
    }

    @Override
    public String nextNodeId() {
        return mapper.nextNodeId();
    }

    @Override
    public String nextReminderId() {
        return mapper.nextReminderId();
    }

    @Override
    public Optional<PlanReminderPolicy> findReminderPolicy(String planId) {
        Objects.requireNonNull(planId, "planId");
        PlanEntity entity = mapper.findPlanById(planId);
        if (entity == null) {
            return Optional.empty();
        }
        List<PlanReminderRuleEntity> rules = mapper.findReminderRulesByPlanIds(List.of(planId));
        PlanReminderPolicy policy = PlanPersistenceMapper.toReminderPolicy(entity, rules);
        return Optional.of(policy);
    }

    @Override
    public void replaceReminderPolicy(String planId, PlanReminderPolicy policy) {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(policy, "policy");
        mapper.deleteReminderRules(planId);
        List<PlanReminderRuleEntity> rules = PlanPersistenceMapper.toReminderRuleEntities(planId, policy.getRules());
        if (!rules.isEmpty()) {
            mapper.insertReminderRules(new ArrayList<>(rules));
        }
        mapper.updateReminderAudit(planId, policy.getUpdatedAt(), policy.getUpdatedBy());
    }

    @Override
    public List<PlanActivity> findTimeline(String planId) {
        Objects.requireNonNull(planId, "planId");
        List<PlanActivityEntity> activities = mapper.findActivitiesByPlanIds(List.of(planId));
        return PlanPersistenceMapper.toActivities(activities);
    }

    @Override
    public void replaceTimeline(String planId, List<PlanActivity> activities) {
        Objects.requireNonNull(planId, "planId");
        mapper.deleteActivities(planId);
        List<PlanActivityEntity> entities = PlanPersistenceMapper.toActivityEntities(planId,
                activities == null ? List.of() : activities);
        if (!entities.isEmpty()) {
            mapper.insertActivities(new ArrayList<>(entities));
        }
    }

    @Override
    public Map<String, List<String>> findAttachments(String planId) {
        Objects.requireNonNull(planId, "planId");
        List<PlanNodeAttachmentEntity> attachments = mapper.findAttachmentsByPlanIds(List.of(planId));
        return PlanPersistenceMapper.toAttachmentMap(attachments);
    }

    @Override
    public void replaceAttachments(String planId, Map<String, List<String>> attachments) {
        Objects.requireNonNull(planId, "planId");
        mapper.deleteAttachments(planId);
        List<PlanNodeAttachmentEntity> entities = PlanPersistenceMapper.toAttachmentEntities(planId, attachments);
        if (!entities.isEmpty()) {
            mapper.insertAttachments(new ArrayList<>(entities));
        }
    }

    private void persistAssociations(PlanAggregate aggregate) {
        if (!aggregate.participants().isEmpty()) {
            mapper.insertParticipants(new ArrayList<>(aggregate.participants()));
        }
        if (!aggregate.nodes().isEmpty()) {
            mapper.insertNodes(new ArrayList<>(aggregate.nodes()));
        }
        if (!aggregate.executions().isEmpty()) {
            mapper.insertExecutions(new ArrayList<>(aggregate.executions()));
        }
        if (!aggregate.attachments().isEmpty()) {
            mapper.insertAttachments(new ArrayList<>(aggregate.attachments()));
        }
        if (!aggregate.activities().isEmpty()) {
            mapper.insertActivities(new ArrayList<>(aggregate.activities()));
        }
        if (!aggregate.reminderRules().isEmpty()) {
            mapper.insertReminderRules(new ArrayList<>(aggregate.reminderRules()));
        }
    }

    private void cleanupAssociations(String planId) {
        mapper.deleteAttachments(planId);
        mapper.deleteExecutions(planId);
        mapper.deleteNodes(planId);
        mapper.deleteParticipants(planId);
        mapper.deleteActivities(planId);
        mapper.deleteReminderRules(planId);
    }

    private List<PlanAggregate> loadAggregates(List<PlanEntity> planEntities) {
        if (planEntities == null || planEntities.isEmpty()) {
            return List.of();
        }
        List<String> planIds = planEntities.stream().map(PlanEntity::id).toList();
        Map<String, List<PlanParticipantEntity>> participants = groupByPlanId(
                mapper.findParticipantsByPlanIds(planIds), PlanParticipantEntity::planId);
        Map<String, List<PlanNodeEntity>> nodes = groupByPlanId(
                mapper.findNodesByPlanIds(planIds), PlanNodeEntity::planId);
        Map<String, List<PlanNodeExecutionEntity>> executions = groupByPlanId(
                mapper.findExecutionsByPlanIds(planIds), PlanNodeExecutionEntity::planId);
        Map<String, List<PlanNodeAttachmentEntity>> attachments = groupByPlanId(
                mapper.findAttachmentsByPlanIds(planIds), PlanNodeAttachmentEntity::planId);
        Map<String, List<PlanActivityEntity>> activities = groupByPlanId(
                mapper.findActivitiesByPlanIds(planIds), PlanActivityEntity::planId);
        Map<String, List<PlanReminderRuleEntity>> reminderRules = groupByPlanId(
                mapper.findReminderRulesByPlanIds(planIds), PlanReminderRuleEntity::planId);

        Comparator<PlanEntity> comparator = Comparator
                .comparing(PlanEntity::plannedStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PlanEntity::id, Comparator.nullsLast(Comparator.naturalOrder()));

        return planEntities.stream()
                .sorted(comparator)
                .map(entity -> new PlanAggregate(
                        entity,
                        participants.getOrDefault(entity.id(), List.of()),
                        nodes.getOrDefault(entity.id(), List.of()),
                        executions.getOrDefault(entity.id(), List.of()),
                        attachments.getOrDefault(entity.id(), List.of()),
                        activities.getOrDefault(entity.id(), List.of()),
                        reminderRules.getOrDefault(entity.id(), List.of())
                ))
                .toList();
    }

    private <T> Map<String, List<T>> groupByPlanId(List<T> items, Function<T, String> classifier) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        return items.stream().collect(Collectors.groupingBy(classifier));
    }

    private List<Plan> toDomain(List<PlanAggregate> aggregates) {
        if (aggregates.isEmpty()) {
            return List.of();
        }
        return aggregates.stream()
                .map(PlanPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
