package com.bob.mta.modules.plan.domain;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class PlanNodeExecution {

    private final String nodeId;
    private final PlanNodeStatus status;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final String operator;
    private final String result;
    private final String log;
    private final List<String> fileIds;

    public PlanNodeExecution(String nodeId, PlanNodeStatus status, OffsetDateTime startTime,
                             OffsetDateTime endTime, String operator, String result,
                             String log, List<String> fileIds) {
        this.nodeId = nodeId;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.operator = operator;
        this.result = result;
        this.log = log;
        this.fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
    }

    public String getNodeId() {
        return nodeId;
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
        return Collections.unmodifiableList(fileIds);
    }
}
