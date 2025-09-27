package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.file.service.FileService;
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;
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
import java.util.List;
import java.util.Locale;
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
        Plan updated = new Plan(current.getId(), current.getTenantId(), command.getTitle(), command.getDescription(),
                current.getCustomerId(), current.getOwner(), command.getParticipants(), current.getStatus(),
                command.getStartTime(), command.getEndTime(), current.getActualStartTime(), current.getActualEndTime(),
                timezone, nodes, executions, current.getCreatedAt(), now);
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
        Plan updated = current.withStatus(nextStatus, actualStart, null, current.getExecutions(), now);
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
        Plan updated = current.withStatus(PlanStatus.CANCELED, current.getActualStartTime(), now, current.getExecutions(), now);
        plans.put(id, updated);
        return updated;
    }

    @Override
    public PlanNodeExecution startNode(String planId, String nodeId, String operator) {
        Plan current = requirePlan(planId);
        PlanNodeExecution target = findExecution(current, nodeId);
        if (target.getStatus() == PlanNodeStatus.DONE) {
            return target;
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanNodeExecution> executions = replaceExecution(current.getExecutions(), nodeId,
                new PlanNodeExecution(nodeId, PlanNodeStatus.IN_PROGRESS,
                        target.getStartTime() == null ? now : target.getStartTime(), null,
                        operator, target.getResult(), target.getLog(), target.getFileIds()));
        PlanStatus nextStatus = current.getStatus() == PlanStatus.SCHEDULED ? PlanStatus.IN_PROGRESS : current.getStatus();
        OffsetDateTime actualStart = current.getActualStartTime();
        if (nextStatus == PlanStatus.IN_PROGRESS && actualStart == null) {
            actualStart = now;
        }
        Plan updated = current.withStatus(nextStatus, actualStart, null, executions, now);
        plans.put(planId, updated);
        return executions.stream().filter(exec -> exec.getNodeId().equals(nodeId)).findFirst().orElse(target);
    }

    @Override
    public PlanNodeExecution completeNode(String planId, String nodeId, String operator, String result,
                                          String log, List<String> fileIds) {
        Plan current = requirePlan(planId);
        PlanNodeExecution target = findExecution(current, nodeId);
        if (target.getStatus() == PlanNodeStatus.DONE) {
            return target;
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
        Plan updated = current.withStatus(nextStatus, actualStart, actualEnd, executions, now);
        plans.put(planId, updated);
        return executions.stream().filter(exec -> exec.getNodeId().equals(nodeId)).findFirst().orElse(target);
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

    private Plan buildPlan(String id, CreatePlanCommand command, OffsetDateTime now) {
        List<PlanNode> nodes = toNodes(command.getNodes());
        List<PlanNodeExecution> executions = initializeExecutions(nodes);
        return new Plan(id, command.getTenantId(), command.getTitle(), command.getDescription(),
                command.getCustomerId(), command.getOwner(), command.getParticipants(), PlanStatus.DESIGN,
                command.getStartTime(), command.getEndTime(), null, null, command.getTimezone(), nodes, executions,
                now, now);
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

    private List<PlanNodeExecution> replaceExecution(List<PlanNodeExecution> executions, String nodeId,
                                                      PlanNodeExecution replacement) {
        return executions.stream()
                .map(exec -> exec.getNodeId().equals(nodeId) ? replacement : exec)
                .toList();
    }

    private String buildEvent(Plan plan) {
        OffsetDateTime start = plan.getPlannedStartTime();
        OffsetDateTime end = plan.getPlannedEndTime();
        String status = switch (plan.getStatus()) {
            case CANCELED -> "CANCELLED";
            case COMPLETED -> "COMPLETED";
            default -> "CONFIRMED";
        };
        String description = String.format("%s\\n负责人: %s\\n状态: %s",
                plan.getDescription() == null ? "" : escape(plan.getDescription()), plan.getOwner(), plan.getStatus().name());
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
}
