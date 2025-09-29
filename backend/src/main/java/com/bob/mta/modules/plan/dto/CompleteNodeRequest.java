package com.bob.mta.modules.plan.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class CompleteNodeRequest {

    @NotBlank
    private String operatorId;

    @NotBlank
    @JsonAlias({"result", "resultSummary"})
    private String resultSummary;

    private String log;

    private List<String> fileIds = List.of();

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getResult() {
        return resultSummary;
    }

    public void setResult(String result) {
        this.resultSummary = result;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public List<String> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<String> fileIds) {
        this.fileIds = fileIds == null ? List.of() : fileIds;
    }
}
