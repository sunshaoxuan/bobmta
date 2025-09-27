package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class InMemoryPlanService implements PlanService {

    private static final DateTimeFormatter ICS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
            .withZone(ZoneOffset.UTC);

    private final ConcurrentMap<String, Plan> plans = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(5000);
    private final AtomicLong nodeIdGenerator = new AtomicLong(1000);
    private final AtomicLong reminderIdGenerator = new AtomicLong(9000);
    private final FileService fileService;

    public InMemoryPlanService(FileService fileService) {
        this.fileService = fileService;
        seedPlans();
    }

    private void seedPlans() {
        List<PlanNodeCommand> nodes = List.of(
                new PlanNodeCommand(null, "数据库备份", "REMOTE", "admin", 1, 60, "remote-template-1",
                        "连接到客户数据库并执行备份脚本", List.of()),
                new PlanNodeCommand(null, "通知客户", "EMAIL", "operator", 2, 15, "email-template-1",
                        "向客户发送巡检通知邮件", List.of())
        );
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-001",
                "东京医疗季度巡检",
                "季度常规巡检并同步巡检报告",
                "cust-001",
                "admin",
                OffsetDateTime.now().plusDays(3),
                OffsetDateTime.now().plusDays(3).plusHours(4),
                "Asia/Tokyo",
                List.of("admin", "operator"),
                nodes
        );
        Plan plan = buildPlan("PLAN-" + idGenerator.incrementAndGet(), command, OffsetDateTime.now());
        plans.put(plan.getId(), plan);

        CreatePlanCommand command2 = new CreatePlanCommand(
                "tenant-001",
                "大阪制造系统升级",
                "准备新版本部署并执行现场验证",
                "cust-002",
                "operator",
                OffsetDateTime.now().plusWeeks(1),
                OffsetDateTime.now().plusWeeks(1).plusHours(6),
                "Asia/Tokyo",
                List.of("operator"),
                List.of(new PlanNodeCommand(null, "现场巡检", "CHECKLIST", "operator", 1, 180, null,
                        "按检查单逐项确认", List.of()))
        );
        Plan plan2 = buildPlan("PLAN-" + idGenerator.incrementAndGet(), command2, OffsetDateTime.now());
        plans.put(plan2.getId(), plan2);
    }

    @Override
    public List<Plan> listPlans(String customerId, PlanStatus status, OffsetDateTime from, OffsetDateTime to) {
        return plans.values().stream()
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
        String id = "PLAN-" + idGenerator.incrementAndGet();
        OffsetDateTime now = OffsetDateTime.now();
        Plan plan = buildPlan(id, command, now);
        plans.put(id, plan);
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
                "计划定义更新",
                current.getId(),
                attributes(
                        "title", command.getTitle(),
                        "timezone", timezone,
                        "participantCount", command.getParticipants() == null ? null
                                : String.valueOf(command.getParticipants().size())
                )));
        Plan updated = current.withDefinition(nodes, executions, now, command.getStartTime(), command.getEndTime(),
                command.getDescription(), command.getParticipants(), timezone, activities);
        plans.put(id, updated);
        return updated;
    }

    @Override
    public void deletePlan(String id) {
        Plan current = requirePlan(id);
        if (current.getStatus() != PlanStatus.DESIGN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only design plans can be deleted");
        }
        plans.remove(id);
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
                "计划发布",
                current.getId(),
                attributes(
                        "status", nextStatus.name(),
                        "operator", operator
                )));
        Plan updated = current.withStatus(nextStatus, actualStart, null, current.getExecutions(), now,
                null, null, null, activities);
        plans.put(id, updated);
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
                "计划取消",
                current.getId(),
                attributes(
                        "reason", reason,
                        "operator", operator
                )));
        Plan updated = current.withStatus(PlanStatus.CANCELED, null, now, current.getExecutions(), now,
                reason, operator, now, activities);
        plans.put(id, updated);
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
                "节点开始执行",
                nodeId,
                attributes(
                        "nodeName", node.getName(),
                        "assignee", node.getAssignee(),
                        "operator", operator
                )));
        Plan updated = current.withStatus(nextStatus, actualStart, null, executions, now,
                null, null, null, activities);
        plans.put(planId, updated);
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
                "节点完成",
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
                    "计划完成",
                    current.getId(),
                    attributes(
                            "operator", operator
                    )));
        }
        Plan updated = current.withStatus(nextStatus, actualStart, actualEnd, executions, now,
                null, null, null, activities);
        plans.put(planId, updated);
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
                "计划负责人交接",
                current.getId(),
                attributes
        ));
        Plan updated = current.withOwnerAndParticipants(newOwner, updatedParticipants, now, activities);
        plans.put(planId, updated);
        return updated;
    }

    @Override
    public String renderPlanIcs(String planId) {
        Plan plan = requirePlan(planId);
        return wrapCalendar(List.of(buildEvent(plan)));
    }

    @Override
    public String renderTenantCalendar(String tenantId) {
        List<String> events = plans.values().stream()
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
                "更新提醒策略",
                current.getId(),
                attributes(
                        "ruleCount", String.valueOf(normalized.size())
                )));
        Plan updated = current.withReminderPolicy(policy, now, activities);
        plans.put(planId, updated);
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
        List<Plan> filtered = plans.values().stream()
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
                "计划创建",
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
        String nodeId = StringUtils.hasText(command.getId()) ? command.getId() : "NODE-" + nodeIdGenerator.incrementAndGet();
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
        Plan plan = plans.get(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return plan;
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
        StringBuilder descriptionBuilder = new StringBuilder(String.format("%s\\n负责人: %s\\n状态: %s",
                plan.getDescription() == null ? "" : escape(plan.getDescription()),
                plan.getOwner(), plan.getStatus().name()));
        if (plan.getStatus() == PlanStatus.CANCELED) {
            if (StringUtils.hasText(plan.getCancelReason())) {
                descriptionBuilder.append("\\n取消原因: ").append(escape(plan.getCancelReason()));
            }
            if (StringUtils.hasText(plan.getCanceledBy())) {
                descriptionBuilder.append("\\n操作人: ").append(escape(plan.getCanceledBy()));
            }
            if (plan.getCanceledAt() != null) {
                descriptionBuilder.append("\\n取消时间: ")
                        .append(escape(plan.getCanceledAt().toString()));
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
                "计划开始前2小时提醒所有参与人"));
        rules.add(new PlanReminderRule(nextReminderId(), PlanReminderTrigger.BEFORE_PLAN_START, 30,
                List.of("IM", "SMS"), "plan-start-alert", List.of("OWNER"),
                "计划开始前30分钟提醒负责人确认准备情况"));
        rules.add(new PlanReminderRule(nextReminderId(), PlanReminderTrigger.BEFORE_PLAN_END, 15,
                List.of("EMAIL"), "plan-summary-reminder", List.of("OWNER"),
                "计划结束前15分钟提醒负责人准备总结输出"));
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
        return "REM-" + reminderIdGenerator.incrementAndGet();
    }
}
