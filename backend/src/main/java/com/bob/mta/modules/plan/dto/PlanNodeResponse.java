package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PlanNodeResponse {

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
    private final PlanNodeExecutionResponse execution;
    private final List<PlanNodeResponse> children;

    public PlanNodeResponse(String id, String name, String type, String assignee, int order,
                            Integer expectedDurationMinutes, PlanNodeActionType actionType,
                            Integer completionThreshold, String actionRef, String description,
                            PlanNodeExecutionResponse execution, List<PlanNodeResponse> children) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.assignee = assignee;
        this.order = order;
        this.expectedDurationMinutes = expectedDurationMinutes;
        this.actionType = actionType;
        this.completionThreshold = completionThreshold;
        this.actionRef = actionRef;
        this.description = description;
        this.execution = execution;
        this.children = children;
    }

    public static PlanNodeResponse from(PlanNode node, Map<String, PlanNodeExecution> executionIndex,
                                        Function<List<String>, List<PlanNodeAttachmentResponse>> attachmentLoader) {
        PlanNodeExecution execution = executionIndex.get(node.getId());
        List<PlanNodeResponse> childResponses = node.getChildren().stream()
                .map(child -> from(child, executionIndex, attachmentLoader))
                .toList();
        return new PlanNodeResponse(node.getId(), node.getName(), node.getType(), node.getAssignee(),
                node.getOrder(), node.getExpectedDurationMinutes(), node.getActionType(),
                node.getCompletionThreshold(), node.getActionRef(), node.getDescription(),
                PlanNodeExecutionResponse.from(execution, attachmentLoader), childResponses);
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

    public PlanNodeExecutionResponse getExecution() {
        return execution;
    }

    public List<PlanNodeResponse> getChildren() {
        return children;
    }
}
