package com.bob.mta.modules.plan.service.command;

import java.time.OffsetDateTime;
import java.util.List;

public class CreatePlanCommand {

    private final String tenantId;
    private final String title;
    private final String description;
    private final String customerId;
    private final String owner;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final String timezone;
    private final List<String> participants;
    private final List<PlanNodeCommand> nodes;

    public CreatePlanCommand(String tenantId, String title, String description, String customerId, String owner,
                             OffsetDateTime startTime, OffsetDateTime endTime, String timezone,
                             List<String> participants, List<PlanNodeCommand> nodes) {
        this.tenantId = tenantId;
        this.title = title;
        this.description = description;
        this.customerId = customerId;
        this.owner = owner;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = timezone;
        this.participants = participants == null ? List.of() : List.copyOf(participants);
        this.nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getOwner() {
        return owner;
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
