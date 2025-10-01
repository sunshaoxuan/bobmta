package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PlanNodeRequest {

    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String type;

    private String assignee;

    @Min(0)
    private int order;

    private Integer expectedDurationMinutes;

    @NotNull
    private PlanNodeActionType actionType;

    @Min(0)
    @Max(100)
    private Integer completionThreshold;

    private String actionRef;

    private String description;

    @Valid
    private List<PlanNodeRequest> children = List.of();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Integer getExpectedDurationMinutes() {
        return expectedDurationMinutes;
    }

    public PlanNodeActionType getActionType() {
        return actionType;
    }

    public void setActionType(PlanNodeActionType actionType) {
        this.actionType = actionType;
    }

    public Integer getCompletionThreshold() {
        return completionThreshold;
    }

    public void setCompletionThreshold(Integer completionThreshold) {
        this.completionThreshold = completionThreshold;
    }

    public void setExpectedDurationMinutes(Integer expectedDurationMinutes) {
        this.expectedDurationMinutes = expectedDurationMinutes;
    }

    public String getActionRef() {
        return actionRef;
    }

    public void setActionRef(String actionRef) {
        this.actionRef = actionRef;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PlanNodeRequest> getChildren() {
        return children;
    }

    public void setChildren(List<PlanNodeRequest> children) {
        this.children = children == null ? List.of() : children;
    }
}
