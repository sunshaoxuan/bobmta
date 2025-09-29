package com.bob.mta.modules.plan.domain;

import java.util.Collections;
import java.util.List;

public class PlanNode {

    private final String id;
    private final String name;
    private final String type;
    private final String assignee;
    private final int order;
    private final Integer expectedDurationMinutes;
    private final String actionRef;
    private final String description;
    private final List<PlanNode> children;

    public PlanNode(String id, String name, String type, String assignee, int order,
                    Integer expectedDurationMinutes, String actionRef, String description,
                    List<PlanNode> children) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.assignee = assignee;
        this.order = order;
        this.expectedDurationMinutes = expectedDurationMinutes;
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

    public String getActionRef() {
        return actionRef;
    }

    public String getDescription() {
        return description;
    }

    public List<PlanNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public PlanNode withAssignee(String newAssignee) {
        return new PlanNode(id, name, type, newAssignee, order, expectedDurationMinutes, actionRef, description,
                children);
    }
}
