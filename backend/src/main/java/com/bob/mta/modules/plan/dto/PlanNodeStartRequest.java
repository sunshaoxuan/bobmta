package com.bob.mta.modules.plan.dto;

import jakarta.validation.constraints.NotBlank;

public class PlanNodeStartRequest {

    @NotBlank
    private String operatorId;

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
}
