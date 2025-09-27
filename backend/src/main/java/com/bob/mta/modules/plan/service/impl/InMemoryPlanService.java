package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.file.service.FileService;
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderSchedule;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanRepository;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import com.bob.mta.modules.plan.service.command.UpdatePlanCommand;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InMemoryPlanService implements PlanService {

    private static final DateTimeFormatter ICS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
            .withZone(ZoneOffset.UTC);
    private final FileService fileService;
    private final PlanRepository planRepository;

    public InMemoryPlanService(FileService fileService, PlanRepository planRepository) {
        this.fileService = fileService;
        this.planRepository = planRepository;
        seedPlans();
    }

    private void seedPlans() {
        if (!planRepository.findAll().isEmpty()) {
            return;
        }

        List<PlanNodeCommand> nodes = List.of(
                new PlanNodeCommand(null, Localization.text(LocalizationKeys.Seeds.PLAN_NODE_BACKUP_TITLE), "REMOTE",
                        "admin", 1, 60, "remote-template-1",
                        Localization.text(LocalizationKeys.Seeds.PLAN_NODE_BACKUP_DESCRIPTION), List.of()),
                new PlanNodeCommand(null, Localization.text(LocalizationKeys.Seeds.PLAN_NODE_NOTIFY_TITLE), "EMAIL",
                        "operator", 2, 15, "email-template-1",
                        Localization.text(LocalizationKeys.Seeds.PLAN_NODE_NOTIFY_DESCRIPTION), List.of())
        );
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-001",
                Localization.text(LocalizationKeys.Seeds.PLAN_PRIMARY_TITLE),
                Localization.text(LocalizationKeys.Seeds.PLAN_PRIMARY_DESCRIPTION),
                "cust-001",
                "admin",
                OffsetDateTime.now().plusDays(3),
                OffsetDateTime.now().plusDays(3).plusHours(4),
                "Asia/Tokyo",
                List.of("admin", "operator"),
                nodes
        );
        Plan plan = buildPlan(planRepository.nextPlanId(), command, OffsetDateTime.now());
        planRepository.save(plan);

        CreatePlanCommand command2 = new CreatePlanCommand(
                "tenant-001",
                Localization.text(LocalizationKeys.Seeds.PLAN_SECONDARY_TITLE),
                Localization.text(LocalizationKeys.Seeds.PLAN_SECONDARY_DESCRIPTION),
                "cust-002",
                "operator",
                OffsetDateTime.now().plusWeeks(1),
                OffsetDateTime.now().plusWeeks(1).plusHours(6),
                "Asia/Tokyo",
                List.of("operator"),
                List.of(new PlanNodeCommand(null, Localization.text(LocalizationKeys.Seeds.PLAN_SECONDARY_NODE_TITLE),
                        "CHECKLIST", "operator", 1, 180, null,
                        Localization.text(LocalizationKeys.Seeds.PLAN_SECONDARY_NODE_DESCRIPTION), List.of()))
        );
        Plan plan2 = buildPlan(planRepository.nextPlanId(), command2, OffsetDateTime.now());
        planRepository.save(plan2);
    }

    @Override
    public List<Plan> listPlans(String customerId, PlanStatus status, OffsetDateTime from, OffsetDateTime to) {
        return planRepository.findAll().stream()
                .filter(plan -> !StringUtils.hasText(customerId) || Objects.equals(plan.getCustomerId(), customerId))
                .filter(plan -> status == null || plan.getStatus() == status)
                .filter(plan -> from == null || !plan.getPlannedEndTime().isBefore(from))
                .filter(plan -> to == null || !plan.getPlannedStartTime().isAfter(to))
                .sorted(Comparator.comparing(Plan::getPlannedStartTime))
                .collect(Collectors.toList());
    }

    @Override
    public Plan getPlan(String id) {
        return requirePlan(id);
    }

    @Override
    public Plan createPlan(CreatePlanCommand command) {
        String id = planRepository.nextPlanId();
        OffsetDateTime now = OffsetDateTime.now();
        Plan plan = buildPlan(id, command, now);
        planRepository.save(plan);
        return plan;
    }

    @Override
    public Plan updatePlan(String id, UpdatePlanCommand command) {
        Plan current = requirePlan(id);
        if (current.getStatus() != PlanStatus.DESIGN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Plan can only be updated while in DESIGN status");
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanNode> nodes = toNodes(command.getNodes());
        List<PlanNodeExecution> executions = initializeExecutions(nodes);
        String timezone = StringUtils.hasText(command.getTimezone()) ? command.getTimezone() : current.getTimezone();
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.PLAN_UPDATED,
                now,
                null,
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_DEFINITION_UPDATED),
                current.getId(),
                attributes(
                        "title", command.getTitle(),
                        "timezone", timezone,
                        "participantCount", command.getParticipants() == null ? null
                                : String.valueOf(command.getParticipants().size())
                )));
        Plan updated = current.withDefinition(nodes, executions, now, command.getStartTime(), command.getEndTime(),
                command.getDescription(), command.getParticipants(), timezone, activities);
        planRepository.save(updated);
        return updated;
    }

    @Override
    public void deletePlan(String id) {
        Plan current = requirePlan(id);
        if (current.getStatus() != PlanStatus.DESIGN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only design plans can be deleted");
        }
        planRepository.delete(id);
    }

    @Override
    public Plan publishPlan(String id, String operator) {
        Plan current = requirePlan(id);
        if (current.getStatus() != PlanStatus.DESIGN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Plan already published");
        }
        OffsetDateTime now = OffsetDateTime.now();
        PlanStatus nextStatus = current.getPlannedStartTime().isAfter(now) ? PlanStatus.SCHEDULED : PlanStatus.IN_PROGRESS;
        OffsetDateTime actualStart = nextStatus == PlanStatus.IN_PROGRESS ? now : current.getActualStartTime();
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.PLAN_PUBLISHED,
                now,
                operator,
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_PUBLISHED),
                current.getId(),
                attributes(
                        "status", nextStatus.name(),
                        "operator", operator
                )));
        Plan updated = current.withStatus(nextStatus, actualStart, null, current.getExecutions(), now,
                null, null, null, activities);
        planRepository.save(updated);
        return updated;
    }

    @Override
    public Plan cancelPlan(String id, String operator, String reason) {
        Plan current = requirePlan(id);
        if (current.getStatus() == PlanStatus.COMPLETED || current.getStatus() == PlanStatus.CANCELED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Plan already completed or canceled");
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.PLAN_CANCELLED,
                now,
                operator,
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_CANCELLED),
                current.getId(),
                attributes(
                        "reason", reason,
                        "operator", operator
                )));
        Plan updated = current.withStatus(PlanStatus.CANCELED, null, now, current.getExecutions(), now,
                reason, operator, now, activities);
        planRepository.save(updated);
        return updated;
    }

    @Override
    public PlanNodeExecution startNode(String planId, String nodeId, String operator) {
        Plan current = requirePlan(planId);
        ensurePlanExecutable(current);
        PlanNodeExecution target = findExecution(current, nodeId);
        if (target.getStatus() == PlanNodeStatus.DONE || target.getStatus() == PlanNodeStatus.IN_PROGRESS) {
            return target;
        }
        OffsetDateTime now = OffsetDateTime.now();
        PlanNode node = findNode(current, nodeId);
        List<PlanNodeExecution> executions = replaceExecution(current.getExecutions(), nodeId,
                new PlanNodeExecution(nodeId, PlanNodeStatus.IN_PROGRESS,
                        target.getStartTime() == null ? now : target.getStartTime(), null,
                        operator, target.getResult(), target.getLog(), target.getFileIds()));
        PlanStatus nextStatus = current.getStatus() == PlanStatus.SCHEDULED ? PlanStatus.IN_PROGRESS : current.getStatus();
        OffsetDateTime actualStart = current.getActualStartTime();
        if (nextStatus == PlanStatus.IN_PROGRESS && actualStart == null) {
            actualStart = now;
        }
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.NODE_STARTED,
                now,
                operator,
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_NODE_STARTED),
                nodeId,
                attributes(
                        "nodeName", node.getName(),
                        "assignee", node.getAssignee(),
                        "operator", operator
                )));
        Plan updated = current.withStatus(nextStatus, actualStart, null, executions, now,
                null, null, null, activities);
        planRepository.save(updated);
        return executions.stream().filter(exec -> exec.getNodeId().equals(nodeId)).findFirst().orElse(target);
    }

    @Override
    public PlanNodeExecution completeNode(String planId, String nodeId, String operator, String result,
                                          String log, List<String> fileIds) {
        Plan current = requirePlan(planId);
        ensurePlanExecutable(current);
        PlanNodeExecution target = findExecution(current, nodeId);
        if (target.getStatus() == PlanNodeStatus.DONE) {
            return target;
        }
        if (target.getStatus() != PlanNodeStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Node must be started before completion");
        }
        if (fileIds != null) {
            fileIds.forEach(fileService::get);
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<String> safeFiles = fileIds == null ? target.getFileIds() : fileIds;
        OffsetDateTime startTime = target.getStartTime() != null ? target.getStartTime() : now;
        List<PlanNodeExecution> executions = replaceExecution(current.getExecutions(), nodeId,
                new PlanNodeExecution(nodeId, PlanNodeStatus.DONE, startTime, now, operator, result, log, safeFiles));
        boolean allDone = executions.stream().allMatch(exec -> exec.getStatus() == PlanNodeStatus.DONE);
        PlanStatus nextStatus = allDone ? PlanStatus.COMPLETED : current.getStatus();
        OffsetDateTime actualStart = current.getActualStartTime() != null ? current.getActualStartTime() : startTime;
        OffsetDateTime actualEnd = allDone ? now : current.getActualEndTime();
        PlanNode node = findNode(current, nodeId);
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.NODE_COMPLETED,
                now,
                operator,
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_NODE_COMPLETED),
                nodeId,
                attributes(
                        "nodeName", node.getName(),
                        "operator", operator,
                        "result", result
                )));
        if (allDone) {
            activities = appendActivity(activities, new PlanActivity(
                    PlanActivityType.PLAN_COMPLETED,
                    now,
                    operator,
                    Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_COMPLETED),
                    current.getId(),
                    attributes(
                            "operator", operator
                    )));
        }
        Plan updated = current.withStatus(nextStatus, actualStart, actualEnd, executions, now,
                null, null, null, activities);
        planRepository.save(updated);
        return executions.stream().filter(exec -> exec.getNodeId().equals(nodeId)).findFirst().orElse(target);
    }

    @Override
    public Plan handoverPlan(String planId, String newOwner, List<String> participants, String note, String operator) {
        Plan current = requirePlan(planId);
        if (!StringUtils.hasText(newOwner)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "New owner is required for handover");
        }
        if (current.getStatus() == PlanStatus.CANCELED || current.getStatus() == PlanStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Plan is no longer active");
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<String> updatedParticipants = participants == null || participants.isEmpty()
                ? current.getParticipants()
                : List.copyOf(participants);
        Map<String, String> attributes = attributes(
                "oldOwner", current.getOwner(),
                "newOwner", newOwner,
                "operator", operator,
                "participantCount", String.valueOf(updatedParticipants.size()),
                "note", StringUtils.hasText(note) ? note : null
        );
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.PLAN_HANDOVER,
                now,
                operator,
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_HANDOVER),
                current.getId(),
                attributes
        ));
        Plan updated = current.withOwnerAndParticipants(newOwner, updatedParticipants, now, activities);
        planRepository.save(updated);
        return updated;
    }

    @Override
    public String renderPlanIcs(String planId) {
        Plan plan = requirePlan(planId);
        return wrapCalendar(List.of(buildEvent(plan)));
    }

    @Override
    public String renderTenantCalendar(String tenantId) {
        List<String> events = planRepository.findAll().stream()
                .filter(plan -> Objects.equals(plan.getTenantId(), tenantId))
                .filter(plan -> plan.getStatus() != PlanStatus.CANCELED)
                .map(this::buildEvent)
                .toList();
        return wrapCalendar(events);
    }

    @Override
    public List<PlanActivity> getPlanTimeline(String planId) {
        Plan plan = requirePlan(planId);
        return plan.getActivities();
    }

    @Override
    public Plan updateReminderPolicy(String planId, List<PlanReminderRule> rules, String operator) {
        Plan current = requirePlan(planId);
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanReminderRule> normalized = normalizeReminderRules(rules);
        PlanReminderPolicy policy = current.getReminderPolicy().withRules(normalized, now, operator);
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.REMINDER_POLICY_UPDATED,
                now,
                operator,
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_REMINDER_UPDATED),
                current.getId(),
                attributes(
                        "ruleCount", String.valueOf(normalized.size())
                )));
        Plan updated = current.withReminderPolicy(policy, now, activities);
        planRepository.save(updated);
        return updated;
    }

    @Override
    public List<PlanReminderSchedule> previewReminderSchedule(String planId, OffsetDateTime referenceTime) {
        Plan plan = requirePlan(planId);
        OffsetDateTime baseline = referenceTime == null ? OffsetDateTime.now() : referenceTime;
        List<PlanReminderSchedule> schedule = new ArrayList<>();
        for (PlanReminderRule rule : plan.getReminderPolicy().getRules()) {
            OffsetDateTime fireTime = computeReminderFireTime(plan, rule);
            if (fireTime == null || fireTime.isBefore(baseline)) {
                continue;
            }
            schedule.add(new PlanReminderSchedule(planId, rule, fireTime));
        }
        schedule.sort(Comparator.comparing(PlanReminderSchedule::getFireTime)
                .thenComparing(entry -> entry.getRule().getOffsetMinutes()));
        return schedule;
    }

    @Override
    public PlanAnalytics getAnalytics(String tenantId, OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Plan> filtered = planRepository.findAll().stream()
                .filter(plan -> tenantId == null || Objects.equals(plan.getTenantId(), tenantId))
                .filter(plan -> from == null || (plan.getPlannedEndTime() != null && !plan.getPlannedEndTime().isBefore(from)))
                .filter(plan -> to == null || (plan.getPlannedStartTime() != null && !plan.getPlannedStartTime().isAfter(to)))
                .sorted(Comparator.comparing(Plan::getPlannedStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        long design = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.DESIGN).count();
        long scheduled = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.SCHEDULED).count();
        long inProgress = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.IN_PROGRESS).count();
        long completed = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.COMPLETED).count();
        long canceled = filtered.stream().filter(plan -> plan.getStatus() == PlanStatus.CANCELED).count();
        long overdue = filtered.stream().filter(plan -> isOverdue(plan, now)).count();

        List<PlanAnalytics.UpcomingPlan> upcoming = filtered.stream()
                .filter(plan -> plan.getPlannedStartTime() != null)
                .filter(plan -> plan.getStatus() != PlanStatus.CANCELED && plan.getStatus() != PlanStatus.COMPLETED)
                .filter(plan -> !plan.getPlannedStartTime().isBefore(now))
                .sorted(Comparator.comparing(Plan::getPlannedStartTime))
                .limit(5)
                .map(plan -> new PlanAnalytics.UpcomingPlan(
                        plan.getId(),
                        plan.getTitle(),
                        plan.getStatus(),
                        plan.getPlannedStartTime(),
                        plan.getPlannedEndTime(),
                        plan.getOwner(),
                        plan.getCustomerId(),
                        plan.getProgress()
                ))
                .toList();

        return new PlanAnalytics(filtered.size(), design, scheduled, inProgress, completed, canceled, overdue, upcoming);
    }

    private boolean isOverdue(Plan plan, OffsetDateTime reference) {
        if (plan.getStatus() == PlanStatus.CANCELED || plan.getStatus() == PlanStatus.COMPLETED) {
            return false;
        }
        if (plan.getStatus() == PlanStatus.DESIGN) {
            return false;
        }
        if (plan.getPlannedEndTime() == null) {
            return false;
        }
        return plan.getPlannedEndTime().isBefore(reference);
    }

    private Plan buildPlan(String id, CreatePlanCommand command, OffsetDateTime now) {
        List<PlanNode> nodes = toNodes(command.getNodes());
        List<PlanNodeExecution> executions = initializeExecutions(nodes);
        List<PlanActivity> activities = List.of(new PlanActivity(
                PlanActivityType.PLAN_CREATED,
                now,
                command.getOwner(),
                Localization.text(LocalizationKeys.Seeds.PLAN_ACTIVITY_CREATED),
                id,
                attributes(
                        "title", command.getTitle(),
                        "owner", command.getOwner()
                )));
        PlanReminderPolicy reminderPolicy = new PlanReminderPolicy(defaultReminderRules(), now, command.getOwner());
        return new Plan(id, command.getTenantId(), command.getTitle(), command.getDescription(),
                command.getCustomerId(), command.getOwner(), command.getParticipants(), PlanStatus.DESIGN,
                command.getStartTime(), command.getEndTime(), null, null, null, null, null,
                command.getTimezone(), nodes, executions, now, now, activities, reminderPolicy);
    }

    private List<PlanNode> toNodes(List<PlanNodeCommand> commands) {
        List<PlanNode> nodes = new ArrayList<>();
        for (PlanNodeCommand command : commands) {
            nodes.add(toNode(command));
        }
        nodes.sort(Comparator.comparingInt(PlanNode::getOrder));
        return nodes;
    }

    private PlanNode toNode(PlanNodeCommand command) {
        List<PlanNode> children = toNodes(command.getChildren());
        String nodeId = StringUtils.hasText(command.getId()) ? command.getId() : planRepository.nextNodeId();
        return new PlanNode(nodeId, command.getName(), command.getType(), command.getAssignee(), command.getOrder(),
                command.getExpectedDurationMinutes(), command.getActionRef(), command.getDescription(), children);
    }

    private List<PlanNodeExecution> initializeExecutions(List<PlanNode> nodes) {
        return flatten(nodes).stream()
                .map(node -> new PlanNodeExecution(node.getId(), PlanNodeStatus.PENDING, null, null, null, null, null, List.of()))
                .toList();
    }

    private List<PlanNode> flatten(List<PlanNode> nodes) {
        List<PlanNode> all = new ArrayList<>();
        for (PlanNode node : nodes) {
            all.add(node);
            all.addAll(flatten(node.getChildren()));
        }
        return all;
    }

    private Plan requirePlan(String id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private PlanNodeExecution findExecution(Plan plan, String nodeId) {
        return plan.getExecutions().stream()
                .filter(exec -> exec.getNodeId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private PlanNode findNode(Plan plan, String nodeId) {
        return flatten(plan.getNodes()).stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private void ensurePlanExecutable(Plan plan) {
        if (plan.getStatus() == PlanStatus.DESIGN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Plan must be published before executing nodes");
        }
        if (plan.getStatus() == PlanStatus.CANCELED || plan.getStatus() == PlanStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Plan is no longer active");
        }
    }

    private List<PlanNodeExecution> replaceExecution(List<PlanNodeExecution> executions, String nodeId,
                                                      PlanNodeExecution replacement) {
        return executions.stream()
                .map(exec -> exec.getNodeId().equals(nodeId) ? replacement : exec)
                .toList();
    }

    private List<PlanActivity> appendActivity(Plan plan, PlanActivity activity) {
        return appendActivity(plan.getActivities(), activity);
    }

    private List<PlanActivity> appendActivity(List<PlanActivity> current, PlanActivity activity) {
        List<PlanActivity> activities = new ArrayList<>(current);
        activities.add(activity);
        activities.sort(Comparator.comparing(PlanActivity::getOccurredAt));
        return activities;
    }

    private Map<String, String> attributes(String... keyValues) {
        Map<String, String> attributes = new HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            if (key != null && value != null) {
                attributes.put(key, value);
            }
        }
        return attributes;
    }

    private String buildEvent(Plan plan) {
        OffsetDateTime start = plan.getPlannedStartTime();
        OffsetDateTime end = plan.getPlannedEndTime();
        String status = switch (plan.getStatus()) {
            case CANCELED -> "CANCELLED";
            case COMPLETED -> "COMPLETED";
            default -> "CONFIRMED";
        };
        String descriptionValue = plan.getDescription() == null ? "" : escape(plan.getDescription());
        StringBuilder descriptionBuilder = new StringBuilder(descriptionValue);
        if (!descriptionValue.isEmpty()) {
            descriptionBuilder.append("\\n");
        }
        descriptionBuilder.append(Localization.text(LocalizationKeys.PlanSummary.RESPONSIBLE_LABEL))
                .append(": ").append(escape(plan.getOwner()));
        descriptionBuilder.append("\\n")
                .append(Localization.text(LocalizationKeys.PlanSummary.STATUS_LABEL))
                .append(": ").append(plan.getStatus().name());
        if (plan.getStatus() == PlanStatus.CANCELED) {
            if (StringUtils.hasText(plan.getCancelReason())) {
                descriptionBuilder.append("\\n")
                        .append(Localization.text(LocalizationKeys.PlanSummary.CANCEL_REASON_LABEL))
                        .append(": ").append(escape(plan.getCancelReason()));
            }
            if (StringUtils.hasText(plan.getCanceledBy())) {
                descriptionBuilder.append("\\n")
                        .append(Localization.text(LocalizationKeys.PlanSummary.CANCEL_OPERATOR_LABEL))
                        .append(": ").append(escape(plan.getCanceledBy()));
            }
            if (plan.getCanceledAt() != null) {
                descriptionBuilder.append("\\n")
                        .append(Localization.text(LocalizationKeys.PlanSummary.CANCEL_TIME_LABEL))
                        .append(": ").append(escape(plan.getCanceledAt().toString()));
            }
        }
        String description = descriptionBuilder.toString();
        return "BEGIN:VEVENT\n" +
                "UID:" + plan.getId() + "@bob-mta.local\n" +
                "DTSTAMP:" + ICS_FORMATTER.format(OffsetDateTime.now()) + "\n" +
                "DTSTART:" + ICS_FORMATTER.format(start) + "\n" +
                "DTEND:" + ICS_FORMATTER.format(end) + "\n" +
                "SUMMARY:" + escape(plan.getTitle()) + "\n" +
                "DESCRIPTION:" + description + "\n" +
                "STATUS:" + status + "\n" +
                "END:VEVENT";
    }

    private String wrapCalendar(List<String> events) {
        String body = events.isEmpty() ? "" : String.join("\n", events) + "\n";
        return "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "PRODID:-//BOB MTA//EN\n" +
                body +
                "END:VCALENDAR\n";
    }

    private String escape(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }

    private List<PlanReminderRule> defaultReminderRules() {
        List<PlanReminderRule> rules = new ArrayList<>();
        rules.add(new PlanReminderRule(nextReminderId(), PlanReminderTrigger.BEFORE_PLAN_START, 120,
                List.of("EMAIL"), "plan-start-email", List.of("PARTICIPANTS"),
                Localization.text(LocalizationKeys.Seeds.PLAN_REMINDER_FIRST)));
        rules.add(new PlanReminderRule(nextReminderId(), PlanReminderTrigger.BEFORE_PLAN_START, 30,
                List.of("IM", "SMS"), "plan-start-alert", List.of("OWNER"),
                Localization.text(LocalizationKeys.Seeds.PLAN_REMINDER_SECOND)));
        rules.add(new PlanReminderRule(nextReminderId(), PlanReminderTrigger.BEFORE_PLAN_END, 15,
                List.of("EMAIL"), "plan-summary-reminder", List.of("OWNER"),
                Localization.text(LocalizationKeys.Seeds.PLAN_REMINDER_THIRD)));
        return rules;
    }

    private List<PlanReminderRule> normalizeReminderRules(List<PlanReminderRule> rules) {
        List<PlanReminderRule> normalized = new ArrayList<>();
        if (rules == null) {
            return normalized;
        }
        for (PlanReminderRule rule : rules) {
            if (rule == null) {
                continue;
            }
            if (!StringUtils.hasText(rule.getTemplateId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Reminder templateId is required");
            }
            PlanReminderRule withId = StringUtils.hasText(rule.getId()) ? rule : rule.withId(nextReminderId());
            normalized.add(withId);
        }
        normalized.sort(Comparator.comparing(PlanReminderRule::getTrigger)
                .thenComparing(PlanReminderRule::getOffsetMinutes));
        return normalized;
    }

    private OffsetDateTime computeReminderFireTime(Plan plan, PlanReminderRule rule) {
        return switch (rule.getTrigger()) {
            case BEFORE_PLAN_START -> plan.getPlannedStartTime() == null
                    ? null
                    : plan.getPlannedStartTime().minusMinutes(rule.getOffsetMinutes());
            case BEFORE_PLAN_END -> plan.getPlannedEndTime() == null
                    ? null
                    : plan.getPlannedEndTime().minusMinutes(rule.getOffsetMinutes());
        };
    }

    private String nextReminderId() {
        return planRepository.nextReminderId();
    }
}
