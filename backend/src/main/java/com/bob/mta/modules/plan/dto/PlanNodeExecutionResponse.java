package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;

public class PlanNodeExecutionResponse {

    private final PlanNodeStatus status;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final String operator;
    private final String result;
    private final String log;
    private final List<String> fileIds;
    private final List<PlanNodeAttachmentResponse> attachments;

    public PlanNodeExecutionResponse(PlanNodeStatus status, OffsetDateTime startTime, OffsetDateTime endTime,
                                     String operator, String result, String log, List<String> fileIds,
                                     List<PlanNodeAttachmentResponse> attachments) {
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.operator = operator;
        this.result = result;
        this.log = log;
        this.fileIds = fileIds;
        this.attachments = attachments;
    }

    public static PlanNodeExecutionResponse from(PlanNodeExecution execution) {
        return from(execution, ids -> List.of());
    }

    public static PlanNodeExecutionResponse from(PlanNodeExecution execution,
                                                 Function<List<String>, List<PlanNodeAttachmentResponse>> attachmentLoader) {
        if (execution == null) {
            return new PlanNodeExecutionResponse(PlanNodeStatus.PENDING, null, null, null, null, null,
                    List.of(), List.of());
        }
        List<String> fileIds = execution.getFileIds();
        List<PlanNodeAttachmentResponse> attachments = attachmentLoader.apply(fileIds == null ? List.of() : fileIds);
        return new PlanNodeExecutionResponse(execution.getStatus(), execution.getStartTime(), execution.getEndTime(),
                execution.getOperator(), execution.getResult(), execution.getLog(), execution.getFileIds(),
                attachments);
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

    public List<PlanNodeAttachmentResponse> getAttachments() {
        return attachments;
    }
}
