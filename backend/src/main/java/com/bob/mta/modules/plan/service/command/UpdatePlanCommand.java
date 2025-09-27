package com.bob.mta.modules.plan.service.command;

import java.time.OffsetDateTime;
import java.util.List;

public class UpdatePlanCommand {

    private final String title;
    private final String description;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final String timezone;
    private final List<String> participants;
    private final List<PlanNodeCommand> nodes;

    public UpdatePlanCommand(String title, String description, OffsetDateTime startTime, OffsetDateTime endTime,
                             String timezone, List<String> participants, List<PlanNodeCommand> nodes) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = timezone;
        this.participants = participants == null ? List.of() : List.copyOf(participants);
        this.nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public List<PlanNodeCommand> getNodes() {
        return nodes;
    }
}
