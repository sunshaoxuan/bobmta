package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;

import java.time.OffsetDateTime;
import java.util.List;

public class PlanNodeExecutionResponse {

    private final PlanNodeStatus status;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final String operator;
    private final String result;
    private final String log;
    private final List<String> fileIds;

    public PlanNodeExecutionResponse(PlanNodeStatus status, OffsetDateTime startTime, OffsetDateTime endTime,
                                     String operator, String result, String log, List<String> fileIds) {
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.operator = operator;
        this.result = result;
        this.log = log;
        this.fileIds = fileIds;
    }

    public static PlanNodeExecutionResponse from(PlanNodeExecution execution) {
        if (execution == null) {
            return new PlanNodeExecutionResponse(PlanNodeStatus.PENDING, null, null, null, null, null, List.of());
        }
        return new PlanNodeExecutionResponse(execution.getStatus(), execution.getStartTime(), execution.getEndTime(),
                execution.getOperator(), execution.getResult(), execution.getLog(), execution.getFileIds());
    }

    public PlanNodeStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public String getOperator() {
        return operator;
    }

    public String getResult() {
        return result;
    }

    public String getLog() {
        return log;
    }

    public List<String> getFileIds() {
        return fileIds;
    }
}
