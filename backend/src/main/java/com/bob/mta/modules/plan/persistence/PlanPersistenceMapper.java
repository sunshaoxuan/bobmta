package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanActionHistory;
import com.bob.mta.modules.plan.domain.PlanActionStatus;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.domain.PlanReminderRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PlanPersistenceMapper {

    private PlanPersistenceMapper() {
    }

    public static PlanAggregate toAggregate(Plan plan) {
        Objects.requireNonNull(plan, "plan");
        PlanEntity planEntity = new PlanEntity(
                plan.getId(),
                plan.getTenantId(),
                plan.getCustomerId(),
                plan.getOwner(),
                plan.getTitle(),
                plan.getDescription(),
                plan.getStatus(),
                plan.getPlannedStartTime(),
                plan.getPlannedEndTime(),
                plan.getActualStartTime(),
                plan.getActualEndTime(),
                plan.getCancelReason(),
                plan.getCanceledBy(),
                plan.getCanceledAt(),
                plan.getTimezone(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                plan.getReminderPolicy().getUpdatedAt(),
                plan.getReminderPolicy().getUpdatedBy()
        );

        List<PlanParticipantEntity> participants = plan.getParticipants().stream()
                .map(participant -> new PlanParticipantEntity(plan.getId(), participant))
                .collect(Collectors.toList());

        List<PlanNodeEntity> nodeEntities = new ArrayList<>();
        flattenNodes(plan.getId(), plan.getNodes(), null, nodeEntities);

        List<PlanNodeExecutionEntity> executions = plan.getExecutions().stream()
                .map(execution -> new PlanNodeExecutionEntity(
                        plan.getId(),
                        execution.getNodeId(),
                        execution.getStatus(),
                        execution.getStartTime(),
                        execution.getEndTime(),
                        execution.getOperator(),
                        execution.getResult(),
                        execution.getLog()
                ))
                .collect(Collectors.toList());

        List<PlanNodeAttachmentEntity> attachments = plan.getExecutions().stream()
                .flatMap(execution -> execution.getFileIds().stream()
                        .map(fileId -> new PlanNodeAttachmentEntity(plan.getId(), execution.getNodeId(), fileId)))
                .collect(Collectors.toList());

        List<PlanActivityEntity> activities = toActivityEntities(plan.getId(), plan.getActivities());

        List<PlanReminderRuleEntity> reminderRules = toReminderRuleEntities(plan.getId(),
                plan.getReminderPolicy().getRules());

        return new PlanAggregate(planEntity, participants, nodeEntities, executions, attachments, activities, reminderRules);
    }

    public static Plan toDomain(PlanAggregate aggregate) {
        Objects.requireNonNull(aggregate, "aggregate");
        PlanEntity entity = aggregate.plan();

        List<String> participants = aggregate.participants().stream()
                .map(PlanParticipantEntity::participantId)
                .collect(Collectors.toList());

        Map<String, List<String>> attachmentsByNode = toAttachmentMap(aggregate.attachments());

        List<PlanNodeExecution> executions = aggregate.executions().stream()
                .map(execution -> new PlanNodeExecution(
                        execution.nodeId(),
                        execution.status(),
                        execution.startTime(),
                        execution.endTime(),
                        execution.operator(),
                        execution.result(),
                        execution.log(),
                        attachmentsByNode.getOrDefault(execution.nodeId(), List.of())
                ))
                .collect(Collectors.toList());

        List<PlanNode> nodes = buildNodeTree(aggregate.nodes());

        List<PlanActivity> activities = toActivities(aggregate.activities());

        PlanReminderPolicy reminderPolicy = toReminderPolicy(entity, aggregate.reminderRules());

        return new Plan(
                entity.id(),
                entity.tenantId(),
                entity.title(),
                entity.description(),
                entity.customerId(),
                entity.owner(),
                participants,
                entity.status(),
                entity.plannedStartTime(),
                entity.plannedEndTime(),
                entity.actualStartTime(),
                entity.actualEndTime(),
                entity.cancelReason(),
                entity.canceledBy(),
                entity.canceledAt(),
                entity.timezone(),
                nodes,
                executions,
                entity.createdAt(),
                entity.updatedAt(),
                activities,
                reminderPolicy
        );
    }

    private static void flattenNodes(String planId, List<PlanNode> nodes, String parentId, List<PlanNodeEntity> collector) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (PlanNode node : nodes) {
            collector.add(new PlanNodeEntity(
                    planId,
                    node.getId(),
                    parentId,
                    node.getName(),
                    node.getType(),
                    node.getAssignee(),
                    node.getOrder(),
                    node.getExpectedDurationMinutes(),
                    node.getActionType(),
                    node.getCompletionThreshold(),
                    node.getActionRef(),
                    node.getDescription()
            ));
            flattenNodes(planId, node.getChildren(), node.getId(), collector);
        }
    }

    private static List<PlanNode> buildNodeTree(List<PlanNodeEntity> nodeEntities) {
        if (nodeEntities == null || nodeEntities.isEmpty()) {
            return List.of();
        }
        Map<String, List<PlanNodeEntity>> grouped = new HashMap<>();
        for (PlanNodeEntity entity : nodeEntities) {
            grouped.computeIfAbsent(entity.parentNodeId(), key -> new ArrayList<>()).add(entity);
        }
        for (List<PlanNodeEntity> entries : grouped.values()) {
            entries.sort(Comparator.comparingInt(PlanNodeEntity::orderIndex));
        }
        return grouped.getOrDefault(null, List.of()).stream()
                .sorted(Comparator.comparingInt(PlanNodeEntity::orderIndex))
                .map(entity -> toDomainNode(entity, grouped))
                .collect(Collectors.toList());
    }

    private static PlanNode toDomainNode(PlanNodeEntity entity, Map<String, List<PlanNodeEntity>> grouped) {
        List<PlanNode> children = grouped.getOrDefault(entity.nodeId(), List.of()).stream()
                .sorted(Comparator.comparingInt(PlanNodeEntity::orderIndex))
                .map(child -> toDomainNode(child, grouped))
                .collect(Collectors.toList());
        return new PlanNode(
                entity.nodeId(),
                entity.name(),
                entity.type(),
                entity.assignee(),
                entity.orderIndex(),
                entity.expectedDurationMinutes(),
                entity.actionType(),
                entity.completionThreshold(),
                entity.actionRef(),
                entity.description(),
                children
        );
    }

    public static List<PlanActivityEntity> toActivityEntities(String planId, List<PlanActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return List.of();
        }
        List<PlanActivityEntity> entities = new ArrayList<>();
        for (int index = 0; index < activities.size(); index++) {
            PlanActivity activity = activities.get(index);
            String activityId = planId + "-activity-" + (index + 1);
            entities.add(new PlanActivityEntity(
                    planId,
                    activityId,
                    activity.getType(),
                    activity.getOccurredAt(),
                    activity.getActor(),
                    activity.getMessage(),
                    activity.getReferenceId(),
                    activity.getAttributes()
            ));
        }
        return entities;
    }

    public static List<PlanActivity> toActivities(List<PlanActivityEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .sorted(Comparator.comparing(PlanActivityEntity::occurredAt)
                        .thenComparing(PlanActivityEntity::activityId))
                .map(activity -> new PlanActivity(
                        activity.type(),
                        activity.occurredAt(),
                        activity.actor(),
                        activity.message(),
                        activity.referenceId(),
                        activity.attributes()
                ))
                .collect(Collectors.toList());
    }

    public static List<PlanReminderRuleEntity> toReminderRuleEntities(String planId, List<PlanReminderRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .map(rule -> new PlanReminderRuleEntity(
                        planId,
                        rule.getId(),
                        rule.getTrigger(),
                        rule.getOffsetMinutes(),
                        rule.getChannels(),
                        rule.getTemplateId(),
                        rule.getRecipients(),
                        rule.getDescription(),
                        rule.isActive()
                ))
                .collect(Collectors.toList());
    }

    public static List<PlanReminderRuleEntity> toReminderRuleEntities(String planId, PlanReminderPolicy policy) {
        if (policy == null) {
            return List.of();
        }
        return toReminderRuleEntities(planId, policy.getRules());
    }

    public static PlanReminderPolicy toReminderPolicy(PlanEntity entity, List<PlanReminderRuleEntity> reminderRules) {
        List<PlanReminderRule> rules = toReminderRules(reminderRules);
        return new PlanReminderPolicy(rules, entity.reminderUpdatedAt(), entity.reminderUpdatedBy());
    }

    public static Map<String, List<String>> toAttachmentMap(List<PlanNodeAttachmentEntity> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (PlanNodeAttachmentEntity attachment : attachments) {
            grouped.computeIfAbsent(attachment.nodeId(), key -> new ArrayList<>())
                    .add(attachment.fileId());
        }
        return grouped.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    public static PlanActionHistoryEntity toActionHistoryEntity(PlanActionHistory history) {
        if (history == null) {
            return null;
        }
        return new PlanActionHistoryEntity(
                history.getId(),
                history.getPlanId(),
                history.getNodeId(),
                history.getActionType(),
                history.getActionRef(),
                history.getTriggeredAt(),
                history.getTriggeredBy(),
                history.getStatus(),
                history.getMessage(),
                history.getError(),
                history.getContext(),
                history.getMetadata()
        );
    }

    public static PlanActionHistory toActionHistory(PlanActionHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PlanActionHistory(
                entity.actionId(),
                entity.planId(),
                entity.nodeId(),
                entity.actionType() == null ? PlanNodeActionType.NONE : entity.actionType(),
                entity.actionRef(),
                entity.triggeredAt(),
                entity.triggeredBy(),
                entity.status() == null ? PlanActionStatus.SKIPPED : entity.status(),
                entity.messageKey(),
                entity.errorMessage(),
                entity.context(),
                entity.metadata()
        );
    }

    public static List<PlanNodeAttachmentEntity> toAttachmentEntities(String planId,
                                                                      Map<String, List<String>> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        List<PlanNodeAttachmentEntity> entities = new ArrayList<>();
        attachments.forEach((nodeId, fileIds) -> {
            if (nodeId == null || fileIds == null || fileIds.isEmpty()) {
                return;
            }
            for (String fileId : fileIds) {
                if (fileId != null) {
                    entities.add(new PlanNodeAttachmentEntity(planId, nodeId, fileId));
                }
            }
        });
        return entities;
    }

    private static List<PlanReminderRule> toReminderRules(List<PlanReminderRuleEntity> reminderRules) {
        if (reminderRules == null || reminderRules.isEmpty()) {
            return List.of();
        }
        return reminderRules.stream()
                .map(rule -> new PlanReminderRule(
                        rule.ruleId(),
                        rule.trigger(),
                        rule.offsetMinutes(),
                        rule.channels(),
                        rule.templateId(),
                        rule.recipients(),
                        rule.description(),
                        rule.active()
                ))
                .collect(Collectors.toList());
    }
}
