package com.bob.mta.modules.plan.dto;

import jakarta.validation.constraints.NotBlank;

public class PlanNodeHandoverRequest {

    @NotBlank
    private String operatorId;

    @NotBlank
    private String assigneeId;

    private String comment;

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
