package com.bob.mta.modules.plan.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class CompleteNodeRequest {

    @NotBlank
    private String result;

    private String log;

    private List<String> fileIds = List.of();

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
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
