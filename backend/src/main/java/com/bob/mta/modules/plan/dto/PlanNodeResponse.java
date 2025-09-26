package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanNode;

import java.util.List;

public class PlanNodeResponse {

    private final String id;
    private final String name;
    private final String type;
    private final String assignee;
    private final int order;
    private final List<PlanNodeResponse> children;

    public PlanNodeResponse(String id, String name, String type, String assignee, int order,
                            List<PlanNodeResponse> children) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.assignee = assignee;
        this.order = order;
        this.children = children;
    }

    public static PlanNodeResponse from(PlanNode node) {
        List<PlanNodeResponse> childResponses = node.getChildren().stream()
                .map(PlanNodeResponse::from)
                .toList();
        return new PlanNodeResponse(node.getId(), node.getName(), node.getType(), node.getAssignee(), node.getOrder(),
                childResponses);
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

    public List<PlanNodeResponse> getChildren() {
        return children;
    }
}
