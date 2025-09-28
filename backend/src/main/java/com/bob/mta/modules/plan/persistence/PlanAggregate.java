package com.bob.mta.modules.plan.persistence;

import java.util.List;

public record PlanAggregate(
        PlanEntity plan,
        List<PlanParticipantEntity> participants,
        List<PlanNodeEntity> nodes,
        List<PlanNodeExecutionEntity> executions,
        List<PlanNodeAttachmentEntity> attachments,
        List<PlanActivityEntity> activities,
        List<PlanReminderRuleEntity> reminderRules
) {

    public PlanAggregate {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }
        participants = participants == null ? List.of() : List.copyOf(participants);
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        executions = executions == null ? List.of() : List.copyOf(executions);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        activities = activities == null ? List.of() : List.copyOf(activities);
        reminderRules = reminderRules == null ? List.of() : List.copyOf(reminderRules);
    }
}
