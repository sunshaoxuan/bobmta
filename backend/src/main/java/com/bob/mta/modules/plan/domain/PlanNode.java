package com.bob.mta.modules.plan.domain;

import java.util.List;

public class PlanNode {

    private final String id;
    private final String name;
    private final String type;
    private final String assignee;
    private final int order;
    private final List<PlanNode> children;

    public PlanNode(String id, String name, String type, String assignee, int order, List<PlanNode> children) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.assignee = assignee;
        this.order = order;
        this.children = children;
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

    public List<PlanNode> getChildren() {
        return children;
    }
}
