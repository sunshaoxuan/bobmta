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
        String actionRef,
        String description
) {
}
