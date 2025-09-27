package com.bob.mta.modules.plan.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class PlanHandoverRequest {

    @NotBlank
    private String newOwner;

    private List<String> participants;

    private String note;

    public String getNewOwner() {
        return newOwner;
    }

    public void setNewOwner(String newOwner) {
        this.newOwner = newOwner;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
