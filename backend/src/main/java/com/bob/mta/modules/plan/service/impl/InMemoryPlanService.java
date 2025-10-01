package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.i18n.MessageResolver;
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
import com.bob.mta.modules.plan.repository.PlanAggregateRepository;
import com.bob.mta.modules.plan.repository.PlanAnalyticsQuery;
import com.bob.mta.modules.plan.repository.PlanAnalyticsRepository;
import com.bob.mta.modules.plan.repository.PlanAttachmentRepository;
import com.bob.mta.modules.plan.repository.PlanReminderPolicyRepository;
import com.bob.mta.modules.plan.repository.PlanRepository;
import com.bob.mta.modules.plan.repository.PlanSearchCriteria;
import com.bob.mta.modules.plan.repository.PlanTimelineRepository;
import com.bob.mta.modules.plan.service.PlanActivityDescriptor;
import com.bob.mta.modules.plan.service.PlanFilterDescriptor;
import com.bob.mta.modules.plan.service.PlanReminderConfigurationDescriptor;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.plan.service.PlanSearchResult;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import com.bob.mta.modules.plan.service.command.UpdatePlanCommand;
import com.bob.mta.modules.plan.service.PlanActivityDescriptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class InMemoryPlanService implements PlanService {

    private static final DateTimeFormatter ICS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
            .withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter CONFLICT_WINDOW_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US);

    private static final List<PlanStatus> STATUS_ORDER = List.of(
            PlanStatus.DESIGN,
            PlanStatus.SCHEDULED,
            PlanStatus.IN_PROGRESS,
            PlanStatus.COMPLETED,
            PlanStatus.CANCELED
    );

    private static final List<PlanStatus> CONFLICT_STATUSES = List.of(
            PlanStatus.SCHEDULED,
            PlanStatus.IN_PROGRESS
    );

    private static final List<PlanActivityDescriptor> ACTIVITY_DESCRIPTORS = List.of(
            descriptor(PlanActivityType.PLAN_CREATED,
                    List.of("plan.activity.created"),
                    attribute("title", "plan.activity.attr.title"),
                    attribute("owner", "plan.activity.attr.owner")),
            descriptor(PlanActivityType.PLAN_UPDATED,
                    List.of("plan.activity.definitionUpdated"),
                    attribute("title", "plan.activity.attr.title"),
                    attribute("timezone", "plan.activity.attr.timezone"),
                    attribute("participantCount", "plan.activity.attr.participantCount")),
            descriptor(PlanActivityType.PLAN_PUBLISHED,
                    List.of("plan.activity.published"),
                    attribute("status", "plan.activity.attr.status"),
                    attribute("operator", "plan.activity.attr.operator")),
            descriptor(PlanActivityType.PLAN_CANCELLED,
                    List.of("plan.activity.cancelled"),
                    attribute("reason", "plan.activity.attr.reason"),
                    attribute("operator", "plan.activity.attr.operator")),
            descriptor(PlanActivityType.PLAN_COMPLETED,
                    List.of("plan.activity.completed"),
                    attribute("operator", "plan.activity.attr.operator")),
            descriptor(PlanActivityType.PLAN_HANDOVER,
                    List.of("plan.activity.handover"),
                    attribute("oldOwner", "plan.activity.attr.oldOwner"),
                    attribute("newOwner", "plan.activity.attr.newOwner"),
                    attribute("operator", "plan.activity.attr.operator"),
                    attribute("participantCount", "plan.activity.attr.participantCount"),
                    attribute("note", "plan.activity.attr.note")),
            descriptor(PlanActivityType.NODE_STARTED,
                    List.of("plan.activity.nodeStarted"),
                    attribute("nodeName", "plan.activity.attr.nodeName"),
                    attribute("assignee", "plan.activity.attr.assignee"),
                    attribute("operator", "plan.activity.attr.operator")),
            descriptor(PlanActivityType.NODE_COMPLETED,
                    List.of("plan.activity.nodeCompleted"),
                    attribute("nodeName", "plan.activity.attr.nodeName"),
                    attribute("operator", "plan.activity.attr.operator"),
                    attribute("result", "plan.activity.attr.result")),
            descriptor(PlanActivityType.NODE_HANDOVER,
                    List.of("plan.activity.nodeHandover"),
                    attribute("nodeName", "plan.activity.attr.nodeName"),
                    attribute("previousAssignee", "plan.activity.attr.previousAssignee"),
                    attribute("newAssignee", "plan.activity.attr.newAssignee"),
                    attribute("operator", "plan.activity.attr.operator"),
                    attribute("comment", "plan.activity.attr.comment")),
            descriptor(PlanActivityType.NODE_AUTO_COMPLETED,
                    List.of("plan.activity.nodeAutoCompleted"),
                    attribute("nodeName", "plan.activity.attr.nodeName"),
                    attribute("threshold", "plan.activity.attr.threshold"),
                    attribute("completedChildren", "plan.activity.attr.completedChildren"),
                    attribute("totalChildren", "plan.activity.attr.totalChildren")),
            descriptor(PlanActivityType.NODE_SKIPPED,
                    List.of("plan.activity.nodeSkipped"),
                    attribute("nodeName", "plan.activity.attr.nodeName"),
                    attribute("parentNodeId", "plan.activity.attr.parentNodeId"),
                    attribute("parentNode", "plan.activity.attr.parentNode")),
            descriptor(PlanActivityType.REMINDER_POLICY_UPDATED,
                    List.of("plan.activity.reminderUpdated", "plan.activity.reminderRuleUpdated"),
                    attribute("ruleCount", "plan.activity.attr.ruleCount"),
                    attribute("offsetMinutes", "plan.activity.attr.offsetMinutes"),
                    attribute("active", "plan.activity.attr.active"))
    );

    private final FileService fileService;
    private final PlanAggregateRepository planRepository;
    private final PlanAnalyticsRepository planAnalyticsRepository;
    private final MessageResolver messageResolver;

    public InMemoryPlanService(FileService fileService,
                               PlanAggregateRepository planRepository,
                               PlanAnalyticsRepository planAnalyticsRepository,
                               MessageResolver messageResolver) {
        this.fileService = fileService;
        this.planRepository = planRepository;
        this.planAnalyticsRepository = planAnalyticsRepository;
        this.messageResolver = messageResolver;
    }

    private PlanRepository plans() {
        return planRepository.plans();
    }

    private PlanReminderPolicyRepository reminderPolicies() {
        return planRepository.reminderPolicies();
    }

    private PlanTimelineRepository timelines() {
        return planRepository.timelines();
    }

    private PlanAttachmentRepository attachments() {
        return planRepository.attachments();
    }

    @Override
    public PlanSearchResult listPlans(String tenantId, String customerId, String owner, String keyword, PlanStatus status,
                                      OffsetDateTime from, OffsetDateTime to, int page, int size) {
        int sanitizedSize = size <= 0 ? 10 : size;
        int sanitizedPage = Math.max(page, 0);
        int offset = sanitizedPage * sanitizedSize;

        PlanSearchCriteria baseCriteria = PlanSearchCriteria.builder()
                .tenantId(StringUtils.hasText(tenantId) ? tenantId : null)
                .customerId(StringUtils.hasText(customerId) ? customerId : null)
                .owner(StringUtils.hasText(owner) ? owner : null)
                .keyword(StringUtils.hasText(keyword) ? keyword : null)
                .status(status)
                .from(from)
                .to(to)
                .build();

        int total = plans().countByCriteria(baseCriteria);

        PlanSearchCriteria pageCriteria = PlanSearchCriteria.builder()
                .tenantId(baseCriteria.getTenantId())
                .customerId(baseCriteria.getCustomerId())
                .owner(baseCriteria.getOwner())
                .keyword(baseCriteria.getKeyword())
                .status(baseCriteria.getStatus())
                .from(baseCriteria.getFrom())
                .to(baseCriteria.getTo())
                .limit(sanitizedSize)
                .offset(offset)
                .build();

        List<Plan> plans = plans().findByCriteria(pageCriteria).stream()
                .sorted(Comparator.comparing(Plan::getPlannedStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Plan::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        return new PlanSearchResult(plans, total);
    }

    @Override
    public Plan getPlan(String id) {
        return requirePlan(id);
    }

    @Override
    @Transactional
    public Plan createPlan(CreatePlanCommand command) {
        ensureNoConflictsForCreation(command);
        String id = plans().nextPlanId();
        OffsetDateTime now = OffsetDateTime.now();
        Plan plan = buildPlan(id, command, now);
        plans().save(plan);
        persistAggregateState(plan);
        return plan;
    }

    @Override
    @Transactional
    public Plan updatePlan(String id, UpdatePlanCommand command) {
        Plan current = requirePlan(id);
        if (current.getStatus() != PlanStatus.DESIGN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.updateDesignOnly"));
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanNode> nodes = toNodes(command.getNodes());
        List<PlanNodeExecution> executions = initializeExecutions(nodes);
        String timezone = StringUtils.hasText(command.getTimezone()) ? command.getTimezone() : current.getTimezone();
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.PLAN_UPDATED,
                now,
                null,
                message("plan.activity.definitionUpdated"),
                current.getId(),
                attributes(
                        "title", command.getTitle(),
                        "timezone", timezone,
                        "participantCount", command.getParticipants() == null ? null
                                : String.valueOf(command.getParticipants().size())
                )));
        Plan updated = current.withDefinition(nodes, executions, now, command.getStartTime(), command.getEndTime(),
                command.getDescription(), command.getParticipants(), timezone, activities);
        plans().save(updated);
        persistAggregateState(updated);
        return updated;
    }

    @Override
    @Transactional
    public void deletePlan(String id) {
        Plan current = requirePlan(id);
        if (current.getStatus() != PlanStatus.DESIGN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.deleteDesignOnly"));
        }
        plans().delete(id);
    }

    @Override
    @Transactional
    public Plan publishPlan(String id, String operator) {
        Plan current = requirePlan(id);
        if (current.getStatus() != PlanStatus.DESIGN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.planPublished"));
        }
        ensureNoConflictsForPublication(current);
        OffsetDateTime now = OffsetDateTime.now();
        PlanStatus nextStatus = current.getPlannedStartTime().isAfter(now) ? PlanStatus.SCHEDULED : PlanStatus.IN_PROGRESS;
        OffsetDateTime actualStart = nextStatus == PlanStatus.IN_PROGRESS ? now : current.getActualStartTime();
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.PLAN_PUBLISHED,
                now,
                operator,
                message("plan.activity.published"),
                current.getId(),
                attributes(
                        "status", nextStatus.name(),
                        "operator", operator
                )));
        Plan updated = current.withStatus(nextStatus, actualStart, null, current.getExecutions(), now,
                null, null, null, activities);
        plans().save(updated);
        persistAggregateState(updated);
        return updated;
    }

    @Override
    @Transactional
    public Plan cancelPlan(String id, String operator, String reason) {
        Plan current = requirePlan(id);
        if (current.getStatus() == PlanStatus.COMPLETED || current.getStatus() == PlanStatus.CANCELED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.planCompletedOrCanceled"));
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.PLAN_CANCELLED,
                now,
                operator,
                message("plan.activity.cancelled"),
                current.getId(),
                attributes(
                        "reason", reason,
                        "operator", operator
                )));
        Plan updated = current.withStatus(PlanStatus.CANCELED, null, now, current.getExecutions(), now,
                reason, operator, now, activities);
        plans().save(updated);
        persistAggregateState(updated);
        return updated;
    }

    @Override
    @Transactional
    public Plan startNode(String planId, String nodeId, String operator) {
        Plan current = requirePlan(planId);
        ensurePlanExecutable(current);
        PlanNodeExecution target = findExecution(current, nodeId);
        if (target.getStatus() == PlanNodeStatus.DONE || target.getStatus() == PlanNodeStatus.IN_PROGRESS) {
            return current;
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
                message("plan.activity.nodeStarted"),
                nodeId,
                attributes(
                        "nodeName", node.getName(),
                        "assignee", node.getAssignee(),
                        "operator", operator
                )));
        Plan updated = current.withStatus(nextStatus, actualStart, null, executions, now,
                null, null, null, activities);
        plans().save(updated);
        persistAggregateState(updated);
        return updated;
    }

    @Override
    @Transactional
    public Plan completeNode(String planId, String nodeId, String operator, String result,
                             String log, List<String> fileIds) {
        Plan current = requirePlan(planId);
        ensurePlanExecutable(current);
        PlanNodeExecution target = findExecution(current, nodeId);
        if (target.getStatus() == PlanNodeStatus.DONE) {
            return current;
        }
        if (target.getStatus() != PlanNodeStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.nodeMustBeStarted"));
        }
        if (fileIds != null) {
            fileIds.forEach(fileService::get);
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<String> safeFiles = fileIds == null ? target.getFileIds() : fileIds;
        OffsetDateTime startTime = target.getStartTime() != null ? target.getStartTime() : now;
        List<PlanNodeExecution> executions = replaceExecution(current.getExecutions(), nodeId,
                new PlanNodeExecution(nodeId, PlanNodeStatus.DONE, startTime, now, operator, result, log, safeFiles));
        ThresholdAdjustment thresholdAdjustment = applyCompletionThresholds(current, executions, now, operator);
        executions = thresholdAdjustment.executions();
        boolean allDone = executions.stream()
                .allMatch(exec -> exec.getStatus() == PlanNodeStatus.DONE || exec.getStatus() == PlanNodeStatus.SKIPPED);
        PlanStatus nextStatus = allDone ? PlanStatus.COMPLETED : current.getStatus();
        OffsetDateTime actualStart = current.getActualStartTime() != null ? current.getActualStartTime() : startTime;
        OffsetDateTime actualEnd = allDone ? now : current.getActualEndTime();
        PlanNode node = findNode(current, nodeId);
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.NODE_COMPLETED,
                now,
                operator,
                message("plan.activity.nodeCompleted"),
                nodeId,
                attributes(
                        "nodeName", node.getName(),
                        "operator", operator,
                        "result", result
                )));
        for (PlanActivity activity : thresholdAdjustment.activities()) {
            activities = appendActivity(activities, activity);
        }
        if (allDone) {
            activities = appendActivity(activities, new PlanActivity(
                    PlanActivityType.PLAN_COMPLETED,
                    now,
                    operator,
                    message("plan.activity.completed"),
                    current.getId(),
                    attributes(
                            "operator", operator
                    )));
        }
        Plan updated = current.withStatus(nextStatus, actualStart, actualEnd, executions, now,
                null, null, null, activities);
        plans().save(updated);
        persistAggregateState(updated);
        return updated;
    }

    @Override
    @Transactional
    public Plan handoverNode(String planId, String nodeId, String newAssignee, String comment, String operator) {
        Plan current = requirePlan(planId);
        ensurePlanExecutable(current);
        if (!StringUtils.hasText(newAssignee)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.nodeAssigneeRequired"));
        }
        OffsetDateTime now = OffsetDateTime.now();
        PlanNode node = findNode(current, nodeId);
        PlanNode updatedNode = node.withAssignee(newAssignee);
        List<PlanNode> nodes = replaceNode(current.getNodes(), nodeId, updatedNode);
        Map<String, String> attributes = attributes(
                "nodeName", node.getName(),
                "previousAssignee", node.getAssignee(),
                "newAssignee", newAssignee,
                "operator", operator,
                "comment", StringUtils.hasText(comment) ? comment : null
        );
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.NODE_HANDOVER,
                now,
                operator,
                message("plan.activity.nodeHandover"),
                nodeId,
                attributes
        ));
        Plan updated = current.withNodes(nodes, current.getExecutions(), now, activities);
        plans().save(updated);
        persistAggregateState(updated);
        return updated;
    }

    @Override
    @Transactional
    public Plan handoverPlan(String planId, String newOwner, List<String> participants, String note, String operator) {
        Plan current = requirePlan(planId);
        if (!StringUtils.hasText(newOwner)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.handoverOwnerRequired"));
        }
        if (current.getStatus() == PlanStatus.CANCELED || current.getStatus() == PlanStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.planInactive"));
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
                message("plan.activity.handover"),
                current.getId(),
                attributes
        ));
        Plan updated = current.withOwnerAndParticipants(newOwner, updatedParticipants, now, activities);
        plans().save(updated);
        persistAggregateState(updated);
        return updated;
    }

    @Override
    public String renderPlanIcs(String planId) {
        Plan plan = requirePlan(planId);
        return wrapCalendar(List.of(buildEvent(plan)));
    }

    @Override
    public String renderTenantCalendar(String tenantId) {
        List<String> events = plans().findAll().stream()
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
    @Transactional
    public Plan updateReminderPolicy(String planId, List<PlanReminderRule> rules, String operator) {
        Plan current = requirePlan(planId);
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanReminderRule> normalized = normalizeReminderRules(rules);
        PlanReminderPolicy policy = current.getReminderPolicy().withRules(normalized, now, operator);
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.REMINDER_POLICY_UPDATED,
                now,
                operator,
                message("plan.activity.reminderUpdated"),
                current.getId(),
                attributes(
                        "ruleCount", String.valueOf(normalized.size())
                )));
        Plan updated = current.withReminderPolicy(policy, now, activities);
        plans().save(updated);
        persistAggregateState(updated);
        return updated;
    }

    @Override
    @Transactional
    public Plan updateReminderRule(String planId, String reminderId, Boolean active, Integer offsetMinutes, String operator) {
        Plan current = requirePlan(planId);
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanReminderRule> rules = current.getReminderPolicy().getRules();
        PlanReminderRule target = rules.stream()
                .filter(rule -> Objects.equals(rule.getId(), reminderId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        int normalizedOffset = offsetMinutes == null ? target.getOffsetMinutes() : offsetMinutes;
        if (normalizedOffset < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.reminderOffsetNonNegative"));
        }
        boolean nextActive = active == null ? target.isActive() : active;
        PlanReminderRule updatedRule = target.withOffsetMinutes(normalizedOffset).withActive(nextActive);
        List<PlanReminderRule> updatedRules = rules.stream()
                .map(rule -> Objects.equals(rule.getId(), reminderId) ? updatedRule : rule)
                .collect(Collectors.toList());
        PlanReminderPolicy policy = current.getReminderPolicy().withRules(updatedRules, now, operator);
        List<PlanActivity> activities = appendActivity(current, new PlanActivity(
                PlanActivityType.REMINDER_POLICY_UPDATED,
                now,
                operator,
                message("plan.activity.reminderRuleUpdated"),
                reminderId,
                attributes(
                        "offsetMinutes", String.valueOf(normalizedOffset),
                        "active", String.valueOf(nextActive)
                )));
        Plan updated = current.withReminderPolicy(policy, now, activities);
        plans().save(updated);
        persistAggregateState(updated);
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
    public List<Plan> findConflictingPlans(String tenantId, String customerId, String ownerId,
                                           OffsetDateTime start, OffsetDateTime end, String excludePlanId) {
        if (!StringUtils.hasText(tenantId)) {
            return List.of();
        }
        if (!StringUtils.hasText(customerId) && !StringUtils.hasText(ownerId)) {
            return List.of();
        }
        TimeWindow targetWindow = toWindow(start, end);
        if (targetWindow == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<Plan> conflicts = new ArrayList<>();
        if (StringUtils.hasText(customerId)) {
            collectConflicts(conflicts, seen, PlanSearchCriteria.builder()
                    .tenantId(tenantId)
                    .customerId(customerId)
                    .from(targetWindow.start())
                    .to(targetWindow.end())
                    .excludePlanId(excludePlanId)
                    .statuses(CONFLICT_STATUSES)
                    .build(), targetWindow);
        }
        if (StringUtils.hasText(ownerId)) {
            collectConflicts(conflicts, seen, PlanSearchCriteria.builder()
                    .tenantId(tenantId)
                    .owner(ownerId)
                    .from(targetWindow.start())
                    .to(targetWindow.end())
                    .excludePlanId(excludePlanId)
                    .statuses(CONFLICT_STATUSES)
                    .build(), targetWindow);
        }
        return List.copyOf(conflicts);
    }

    @Override
    public PlanAnalytics getAnalytics(String tenantId, String customerId, String ownerId,
                                      OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime reference = OffsetDateTime.now();
        PlanAnalyticsQuery query = PlanAnalyticsQuery.builder()
                .tenantId(StringUtils.hasText(tenantId) ? tenantId : null)
                .customerId(StringUtils.hasText(customerId) ? customerId : null)
                .ownerId(StringUtils.hasText(ownerId) ? ownerId : null)
                .from(from)
                .to(to)
                .referenceTime(reference)
                .upcomingLimit(5)
                .ownerLimit(5)
                .riskLimit(5)
                .dueSoonMinutes(1440)
                .build();
        return planAnalyticsRepository.summarize(query);
    }

    @Override
    public List<PlanActivityDescriptor> describeActivities() {
        return ACTIVITY_DESCRIPTORS;
    }

    private void ensureNoConflictsForCreation(CreatePlanCommand command) {
        if (command == null) {
            return;
        }
        List<Plan> conflicts = findConflictingPlans(command.getTenantId(), command.getCustomerId(),
                command.getOwner(), command.getStartTime(), command.getEndTime(), null);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    message("plan.error.scheduleConflict", summarizeConflicts(conflicts)));
        }
    }

    private void ensureNoConflictsForPublication(Plan plan) {
        if (plan == null) {
            return;
        }
        List<Plan> conflicts = findConflictingPlans(plan.getTenantId(), plan.getCustomerId(), plan.getOwner(),
                plan.getPlannedStartTime(), plan.getPlannedEndTime(), plan.getId());
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    message("plan.warning.scheduleConflict", summarizeConflicts(conflicts)));
        }
    }

    private void collectConflicts(List<Plan> sink, Set<String> seen, PlanSearchCriteria criteria, TimeWindow target) {
        if (criteria == null) {
            return;
        }
        for (Plan plan : plans().findByCriteria(criteria)) {
            if (seen.contains(plan.getId())) {
                continue;
            }
            if (!CONFLICT_STATUSES.contains(plan.getStatus())) {
                continue;
            }
            TimeWindow candidateWindow = toWindow(plan.getPlannedStartTime(), plan.getPlannedEndTime());
            if (candidateWindow == null) {
                continue;
            }
            if (!overlaps(target, candidateWindow)) {
                continue;
            }
            if (seen.add(plan.getId())) {
                sink.add(plan);
            }
        }
    }

    private boolean overlaps(TimeWindow first, TimeWindow second) {
        if (first == null || second == null) {
            return false;
        }
        return !first.end().isBefore(second.start()) && !second.end().isBefore(first.start());
    }

    private TimeWindow toWindow(OffsetDateTime start, OffsetDateTime end) {
        OffsetDateTime effectiveStart = start;
        OffsetDateTime effectiveEnd = end;
        if (effectiveStart == null && effectiveEnd == null) {
            return null;
        }
        if (effectiveStart == null) {
            effectiveStart = effectiveEnd;
        }
        if (effectiveEnd == null) {
            effectiveEnd = effectiveStart;
        }
        if (effectiveEnd.isBefore(effectiveStart)) {
            OffsetDateTime tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }
        return new TimeWindow(effectiveStart, effectiveEnd);
    }

    private String summarizeConflicts(List<Plan> conflicts) {
        return conflicts.stream()
                .map(this::describeConflict)
                .collect(Collectors.joining(", "));
    }

    private String describeConflict(Plan plan) {
        String title = StringUtils.hasText(plan.getTitle()) ? plan.getTitle() : plan.getId();
        TimeWindow window = toWindow(plan.getPlannedStartTime(), plan.getPlannedEndTime());
        if (window == null) {
            return title;
        }
        return title + " [" + formatWindow(window) + "]";
    }

    private String formatWindow(TimeWindow window) {
        String start = CONFLICT_WINDOW_FORMATTER.format(window.start());
        String end = CONFLICT_WINDOW_FORMATTER.format(window.end());
        if (start.equals(end)) {
            return start;
        }
        return start + " - " + end;
    }

    private record TimeWindow(OffsetDateTime start, OffsetDateTime end) { }

    @Override
    public PlanFilterDescriptor describePlanFilters(String tenantId) {
        Stream<Plan> scopedStream = plans().findAll().stream();
        if (StringUtils.hasText(tenantId)) {
            scopedStream = scopedStream.filter(plan -> tenantId.equals(plan.getTenantId()));
        }
        List<Plan> scopedPlans = scopedStream.toList();

        Map<PlanStatus, Long> statusCounts = scopedPlans.stream()
                .collect(Collectors.groupingBy(Plan::getStatus, Collectors.counting()));

        List<PlanFilterDescriptor.Option> statusOptions = STATUS_ORDER.stream()
                .map(status -> new PlanFilterDescriptor.Option(
                        status.name(),
                        message(statusLabelKey(status)),
                        statusCounts.getOrDefault(status, 0L)))
                .toList();

        Map<String, Long> ownerCounts = scopedPlans.stream()
                .map(Plan::getOwner)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<PlanFilterDescriptor.Option> ownerOptions = ownerCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new PlanFilterDescriptor.Option(entry.getKey(), entry.getKey(), entry.getValue()))
                .toList();

        Map<String, Long> customerCounts = scopedPlans.stream()
                .map(Plan::getCustomerId)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<PlanFilterDescriptor.Option> customerOptions = customerCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new PlanFilterDescriptor.Option(entry.getKey(), entry.getKey(), entry.getValue()))
                .toList();

        OffsetDateTime earliestStart = scopedPlans.stream()
                .map(Plan::getPlannedStartTime)
                .filter(Objects::nonNull)
                .min(OffsetDateTime::compareTo)
                .orElse(null);
        OffsetDateTime latestEnd = scopedPlans.stream()
                .map(Plan::getPlannedEndTime)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);

        PlanFilterDescriptor.DateRange window = (earliestStart != null || latestEnd != null)
                ? new PlanFilterDescriptor.DateRange(earliestStart, latestEnd)
                : null;

        return new PlanFilterDescriptor(
                message(LocalizationKeys.PlanFilter.STATUS_LABEL),
                statusOptions,
                message(LocalizationKeys.PlanFilter.OWNER_LABEL),
                ownerOptions,
                message(LocalizationKeys.PlanFilter.CUSTOMER_LABEL),
                customerOptions,
                message(LocalizationKeys.PlanFilter.WINDOW_LABEL),
                message(LocalizationKeys.PlanFilter.WINDOW_HINT),
                window
        );
    }

    @Override
    public PlanReminderConfigurationDescriptor describeReminderOptions() {
        List<PlanReminderConfigurationDescriptor.Option> triggers = List.of(
                new PlanReminderConfigurationDescriptor.Option(
                        PlanReminderTrigger.BEFORE_PLAN_START.name(),
                        LocalizationKeys.PlanReminder.TRIGGER_BEFORE_START,
                        LocalizationKeys.PlanReminder.TRIGGER_BEFORE_START_DESC),
                new PlanReminderConfigurationDescriptor.Option(
                        PlanReminderTrigger.BEFORE_PLAN_END.name(),
                        LocalizationKeys.PlanReminder.TRIGGER_BEFORE_END,
                        LocalizationKeys.PlanReminder.TRIGGER_BEFORE_END_DESC)
        );
        List<PlanReminderConfigurationDescriptor.Option> channels = List.of(
                new PlanReminderConfigurationDescriptor.Option(
                        "EMAIL",
                        LocalizationKeys.PlanReminder.CHANNEL_EMAIL,
                        LocalizationKeys.PlanReminder.CHANNEL_EMAIL_DESC),
                new PlanReminderConfigurationDescriptor.Option(
                        "IM",
                        LocalizationKeys.PlanReminder.CHANNEL_IM,
                        LocalizationKeys.PlanReminder.CHANNEL_IM_DESC),
                new PlanReminderConfigurationDescriptor.Option(
                        "SMS",
                        LocalizationKeys.PlanReminder.CHANNEL_SMS,
                        LocalizationKeys.PlanReminder.CHANNEL_SMS_DESC)
        );
        List<PlanReminderConfigurationDescriptor.Option> recipientGroups = List.of(
                new PlanReminderConfigurationDescriptor.Option(
                        "OWNER",
                        LocalizationKeys.PlanReminder.RECIPIENT_OWNER,
                        LocalizationKeys.PlanReminder.RECIPIENT_OWNER_DESC),
                new PlanReminderConfigurationDescriptor.Option(
                        "PARTICIPANTS",
                        LocalizationKeys.PlanReminder.RECIPIENT_PARTICIPANTS,
                        LocalizationKeys.PlanReminder.RECIPIENT_PARTICIPANTS_DESC),
                new PlanReminderConfigurationDescriptor.Option(
                        "CUSTOM",
                        LocalizationKeys.PlanReminder.RECIPIENT_CUSTOM,
                        LocalizationKeys.PlanReminder.RECIPIENT_CUSTOM_DESC)
        );
        return new PlanReminderConfigurationDescriptor(triggers, channels, recipientGroups, 0, 1440, 60);
    }

    private Plan buildPlan(String id, CreatePlanCommand command, OffsetDateTime now) {
        List<PlanNode> nodes = toNodes(command.getNodes());
        List<PlanNodeExecution> executions = initializeExecutions(nodes);
        List<PlanActivity> activities = List.of(new PlanActivity(
                PlanActivityType.PLAN_CREATED,
                now,
                command.getOwner(),
                message("plan.activity.created"),
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
        String nodeId = StringUtils.hasText(command.getId()) ? command.getId() : plans().nextNodeId();
        return new PlanNode(nodeId, command.getName(), command.getType(), command.getAssignee(), command.getOrder(),
                command.getExpectedDurationMinutes(), command.getActionType(), command.getCompletionThreshold(),
                command.getActionRef(), command.getDescription(), children);
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

    private ThresholdAdjustment applyCompletionThresholds(Plan plan, List<PlanNodeExecution> executions,
                                                           OffsetDateTime now, String operator) {
        if (plan.getNodes().isEmpty()) {
            return new ThresholdAdjustment(executions, List.of());
        }
        Map<String, PlanNodeExecution> executionIndex = executions.stream()
                .collect(Collectors.toMap(PlanNodeExecution::getNodeId, Function.identity()));
        List<PlanNode> allNodes = flatten(plan.getNodes());
        Map<String, List<PlanNode>> childrenIndex = new HashMap<>();
        for (PlanNode node : allNodes) {
            for (PlanNode child : node.getChildren()) {
                childrenIndex.computeIfAbsent(node.getId(), key -> new ArrayList<>()).add(child);
            }
        }

        List<PlanActivity> activities = new ArrayList<>();
        boolean changed;
        do {
            changed = false;
            for (PlanNode node : allNodes) {
                List<PlanNode> children = childrenIndex.getOrDefault(node.getId(), List.of());
                if (children.isEmpty()) {
                    continue;
                }
                PlanNodeExecution parentExec = executionIndex.get(node.getId());
                if (parentExec == null || parentExec.getStatus() == PlanNodeStatus.DONE) {
                    continue;
                }
                int threshold = normalizeThreshold(node.getCompletionThreshold());
                long doneChildren = children.stream()
                        .map(child -> executionIndex.get(child.getId()))
                        .filter(exec -> exec != null && exec.getStatus() == PlanNodeStatus.DONE)
                        .count();
                int totalChildren = children.size();
                double completionRatio = totalChildren == 0 ? 100 : doneChildren * 100.0 / totalChildren;
                if (completionRatio < threshold) {
                    continue;
                }
                if (parentExec.getStatus() != PlanNodeStatus.DONE) {
                    PlanNodeExecution completed = new PlanNodeExecution(parentExec.getNodeId(), PlanNodeStatus.DONE,
                            parentExec.getStartTime() == null ? now : parentExec.getStartTime(),
                            now, operator, parentExec.getResult(), parentExec.getLog(), parentExec.getFileIds());
                    executionIndex.put(parentExec.getNodeId(), completed);
                    activities.add(new PlanActivity(
                            PlanActivityType.NODE_AUTO_COMPLETED,
                            now,
                            operator,
                            message("plan.activity.nodeAutoCompleted"),
                            parentExec.getNodeId(),
                            attributes(
                                    "nodeName", node.getName(),
                                    "threshold", String.valueOf(threshold),
                                    "completedChildren", String.valueOf(doneChildren),
                                    "totalChildren", String.valueOf(totalChildren)
                            )));
                    changed = true;
                }
                for (PlanNode child : children) {
                    PlanNodeExecution childExec = executionIndex.get(child.getId());
                    if (childExec == null || childExec.getStatus() == PlanNodeStatus.DONE
                            || childExec.getStatus() == PlanNodeStatus.SKIPPED) {
                        continue;
                    }
                    PlanNodeExecution skipped = new PlanNodeExecution(childExec.getNodeId(), PlanNodeStatus.SKIPPED,
                            childExec.getStartTime(), now, operator, childExec.getResult(), childExec.getLog(),
                            childExec.getFileIds());
                    executionIndex.put(childExec.getNodeId(), skipped);
                    activities.add(new PlanActivity(
                            PlanActivityType.NODE_SKIPPED,
                            now,
                            operator,
                            message("plan.activity.nodeSkipped"),
                            childExec.getNodeId(),
                            attributes(
                                    "nodeName", child.getName(),
                                    "parentNodeId", node.getId(),
                                    "parentNode", node.getName()
                            )));
                    changed = true;
                }
            }
        } while (changed);

        List<PlanNodeExecution> orderedExecutions = executions.stream()
                .map(exec -> executionIndex.getOrDefault(exec.getNodeId(), exec))
                .toList();
        return new ThresholdAdjustment(orderedExecutions, activities);
    }

    private int normalizeThreshold(Integer threshold) {
        if (threshold == null) {
            return 100;
        }
        if (threshold < 0) {
            return 0;
        }
        return Math.min(threshold, 100);
    }

    private static PlanActivityDescriptor descriptor(PlanActivityType type, List<String> messageKeys,
                                                     PlanActivityDescriptor.ActivityAttribute... attributes) {
        return new PlanActivityDescriptor(type, messageKeys, List.of(attributes));
    }

    private static PlanActivityDescriptor.ActivityAttribute attribute(String name, String descriptionKey) {
        return new PlanActivityDescriptor.ActivityAttribute(name, descriptionKey);
    }

    private record ThresholdAdjustment(List<PlanNodeExecution> executions, List<PlanActivity> activities) {
    }

    private Plan requirePlan(String id) {
        return plans().findById(id)
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
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.planMustBePublished"));
        }
        if (plan.getStatus() == PlanStatus.CANCELED || plan.getStatus() == PlanStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.planInactive"));
        }
    }

    private List<PlanNodeExecution> replaceExecution(List<PlanNodeExecution> executions, String nodeId,
                                                      PlanNodeExecution replacement) {
        return executions.stream()
                .map(exec -> exec.getNodeId().equals(nodeId) ? replacement : exec)
                .toList();
    }

    private List<PlanNode> replaceNode(List<PlanNode> nodes, String nodeId, PlanNode replacement) {
        if (nodes == null || nodes.isEmpty()) {
            return nodes;
        }
        boolean changed = false;
        List<PlanNode> updated = new ArrayList<>(nodes.size());
        for (PlanNode node : nodes) {
            PlanNode next = node;
            if (node.getId().equals(nodeId)) {
                next = replacement;
                changed = true;
            } else {
                List<PlanNode> updatedChildren = replaceNode(node.getChildren(), nodeId, replacement);
                if (updatedChildren != node.getChildren()) {
                    next = new PlanNode(node.getId(), node.getName(), node.getType(), node.getAssignee(),
                            node.getOrder(), node.getExpectedDurationMinutes(), node.getActionType(),
                            node.getCompletionThreshold(), node.getActionRef(), node.getDescription(), updatedChildren);
                    changed = true;
                }
            }
            updated.add(next);
        }
        return changed ? updated : nodes;
    }

    private void persistAggregateState(Plan plan) {
        timelines().replaceTimeline(plan.getId(), plan.getActivities());
        reminderPolicies().replaceReminderPolicy(plan.getId(), plan.getReminderPolicy());
        attachments().replaceAttachments(plan.getId(), collectAttachments(plan.getExecutions()));
    }

    private Map<String, List<String>> collectAttachments(List<PlanNodeExecution> executions) {
        if (executions == null || executions.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> attachments = new LinkedHashMap<>();
        for (PlanNodeExecution execution : executions) {
            if (execution.getFileIds() == null || execution.getFileIds().isEmpty()) {
                continue;
            }
            attachments.put(execution.getNodeId(), List.copyOf(execution.getFileIds()));
        }
        return attachments;
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
        String descriptionHeader = message("plan.ics.description",
                plan.getDescription() == null ? "" : escape(plan.getDescription()),
                escape(plan.getOwner()), plan.getStatus().name());
        StringBuilder descriptionBuilder = new StringBuilder(descriptionHeader);
        if (plan.getStatus() == PlanStatus.CANCELED) {
            if (StringUtils.hasText(plan.getCancelReason())) {
                descriptionBuilder.append(message("plan.ics.cancel.reason", escape(plan.getCancelReason())));
            }
            if (StringUtils.hasText(plan.getCanceledBy())) {
                descriptionBuilder.append(message("plan.ics.cancel.operator", escape(plan.getCanceledBy())));
            }
            if (plan.getCanceledAt() != null) {
                descriptionBuilder.append(message("plan.ics.cancel.time",
                        escape(plan.getCanceledAt().toString())));
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
                message("plan.reminder.default.start2h")));
        rules.add(new PlanReminderRule(nextReminderId(), PlanReminderTrigger.BEFORE_PLAN_START, 30,
                List.of("IM", "SMS"), "plan-start-alert", List.of("OWNER"),
                message("plan.reminder.default.start30m")));
        rules.add(new PlanReminderRule(nextReminderId(), PlanReminderTrigger.BEFORE_PLAN_END, 15,
                List.of("EMAIL"), "plan-summary-reminder", List.of("OWNER"),
                message("plan.reminder.default.end15m")));
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
                throw new BusinessException(ErrorCode.BAD_REQUEST, message("plan.error.reminderTemplateRequired"));
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
        return plans().nextReminderId();
    }

    private String statusLabelKey(PlanStatus status) {
        return switch (status) {
            case DESIGN -> LocalizationKeys.Frontend.PLAN_STATUS_DESIGN;
            case SCHEDULED -> LocalizationKeys.Frontend.PLAN_STATUS_SCHEDULED;
            case IN_PROGRESS -> LocalizationKeys.Frontend.PLAN_STATUS_IN_PROGRESS;
            case COMPLETED -> LocalizationKeys.Frontend.PLAN_STATUS_COMPLETED;
            case CANCELED -> LocalizationKeys.Frontend.PLAN_STATUS_CANCELLED;
        };
    }

    private String message(String code, Object... args) {
        return messageResolver.getMessage(code, args);
    }
}
