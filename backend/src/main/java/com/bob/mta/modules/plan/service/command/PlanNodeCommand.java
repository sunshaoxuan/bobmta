package com.bob.mta.modules.plan.service.command;

import com.bob.mta.modules.plan.domain.PlanNodeActionType;

import java.util.List;

public class PlanNodeCommand {

    private final String id;
    private final String name;
    private final String type;
    private final String assignee;
    private final int order;
    private final Integer expectedDurationMinutes;
    private final PlanNodeActionType actionType;
    private final Integer completionThreshold;
    private final String actionRef;
    private final String description;
    private final List<PlanNodeCommand> children;

    public PlanNodeCommand(String id, String name, String type, String assignee, int order,
                           Integer expectedDurationMinutes, PlanNodeActionType actionType,
                           Integer completionThreshold, String actionRef, String description,
                           List<PlanNodeCommand> children) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.assignee = assignee;
        this.order = order;
        this.expectedDurationMinutes = expectedDurationMinutes;
        this.actionType = actionType == null ? PlanNodeActionType.NONE : actionType;
        this.completionThreshold = completionThreshold == null
                ? 100
                : Math.max(0, Math.min(100, completionThreshold));
        this.actionRef = actionRef;
        this.description = description;
        this.children = children == null ? List.of() : List.copyOf(children);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getAssignee() {
        return assignee;
    }

    public int getOrder() {
        return order;
    }

    public Integer getExpectedDurationMinutes() {
        return expectedDurationMinutes;
    }

    public PlanNodeActionType getActionType() {
        return actionType;
    }

    public Integer getCompletionThreshold() {
        return completionThreshold;
    }

    public String getActionRef() {
        return actionRef;
    }

    public String getDescription() {
        return description;
    }

    public List<PlanNodeCommand> getChildren() {
        return children;
    }
}
