package com.bob.mta.modules.plan.persistence;

public record PlanNodeEntity(
        String planId,
        String nodeId,
        String parentNodeId,
        String name,
        String type,
        String assignee,
        int orderIndex,
        Integer expectedDurationMinutes,
        com.bob.mta.modules.plan.domain.PlanNodeActionType actionType,
        Integer completionThreshold,
        String actionRef,
        String description
) {
}
