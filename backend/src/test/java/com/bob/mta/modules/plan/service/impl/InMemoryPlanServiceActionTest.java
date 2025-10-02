package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.modules.file.domain.FileMetadata;
import com.bob.mta.modules.file.service.FileService;
import com.bob.mta.modules.notification.EmailMessage;
import com.bob.mta.modules.notification.InstantMessage;
import com.bob.mta.modules.notification.NotificationGateway;
import com.bob.mta.modules.notification.NotificationResult;
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActionHistory;
import com.bob.mta.modules.plan.domain.PlanActionStatus;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanActionHistoryRepository;
import com.bob.mta.modules.plan.repository.PlanAggregateRepository;
import com.bob.mta.modules.plan.repository.PlanAnalyticsQuery;
import com.bob.mta.modules.plan.repository.PlanAnalyticsRepository;
import com.bob.mta.modules.plan.repository.PlanAttachmentRepository;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;
import com.bob.mta.modules.plan.repository.PlanBoardWindow;
import com.bob.mta.modules.plan.repository.PlanReminderPolicyRepository;
import com.bob.mta.modules.plan.repository.PlanRepository;
import com.bob.mta.modules.plan.repository.PlanSearchCriteria;
import com.bob.mta.modules.plan.repository.PlanTimelineRepository;
import com.bob.mta.modules.template.domain.RenderedTemplate;
import com.bob.mta.modules.template.service.TemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InMemoryPlanServiceActionTest {

    private TemplateService templateService;
    private NotificationGateway notificationGateway;
    private RecordingPlanActionHistoryRepository actionHistoryRepository;
    private StubPlanAggregateRepository aggregateRepository;
    private PlanAnalyticsRepository planAnalyticsRepository;
    private FileService fileService;
    private MessageResolver messageResolver;
    private InMemoryPlanService planService;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.US);
        templateService = mock(TemplateService.class);
        notificationGateway = mock(NotificationGateway.class);
        actionHistoryRepository = new RecordingPlanActionHistoryRepository();
        aggregateRepository = new StubPlanAggregateRepository();
        planAnalyticsRepository = new NoopPlanAnalyticsRepository();
        fileService = new NoopFileService();
        messageResolver = new MessageResolver(new StaticMessageSource()) {
            @Override
            public String getMessage(String code, Object... args) {
                return code;
            }
        };
        planService = new InMemoryPlanService(
                fileService,
                aggregateRepository,
                planAnalyticsRepository,
                actionHistoryRepository,
                templateService,
                notificationGateway,
                messageResolver
        );
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void startNode_shouldDispatchEmailActionAndRecordSuccess() {
        Plan plan = seedPlan("plan-email-success", PlanStatus.SCHEDULED, PlanNodeStatus.PENDING,
                PlanNodeActionType.EMAIL, "101", "alice");
        aggregateRepository.planRepository.save(plan);
        RenderedTemplate template = new RenderedTemplate(
                "Subject",
                "Body",
                List.of("user@example.com"),
                List.of(),
                "https://endpoint",
                null,
                null,
                null,
                Map.of("templateMeta", "value")
        );
        when(templateService.render(anyLong(), anyMap(), any(Locale.class))).thenReturn(template);
        NotificationResult success = NotificationResult.success("EMAIL", "email.sent", Map.of("provider", "mock"));
        when(notificationGateway.sendEmail(any(EmailMessage.class))).thenReturn(success);

        Plan updated = planService.startNode(plan.getId(), plan.getNodes().get(0).getId(), "operator-1");

        verify(notificationGateway).sendEmail(any(EmailMessage.class));
        assertThat(actionHistoryRepository.entries).hasSize(1);
        PlanActionHistory history = actionHistoryRepository.entries.get(0);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.SUCCESS);
        assertThat(history.getMetadata()).containsEntry("templateId", "101").containsEntry("attempts", "1");
        assertThat(history.getContext()).containsEntry("planId", plan.getId());

        PlanActivity actionActivity = findActionActivity(updated);
        assertThat(actionActivity.getAttributes())
                .containsEntry("actionType", PlanNodeActionType.EMAIL.name())
                .containsEntry("actionStatus", PlanActionStatus.SUCCESS.name())
                .containsEntry("actionMessage", "email.sent")
                .containsEntry("meta.templateId", "101")
                .containsEntry("meta.provider", "mock")
                .containsEntry("context.planId", plan.getId())
                .containsEntry("context.nodeAssignee", "alice")
                .doesNotContainKey("actionError");
    }

    @Test
    void startNode_shouldRecordEmailFailureWhenTemplateThrows() {
        Plan plan = seedPlan("plan-email-failure", PlanStatus.SCHEDULED, PlanNodeStatus.PENDING,
                PlanNodeActionType.EMAIL, "102", "bob");
        aggregateRepository.planRepository.save(plan);
        when(templateService.render(anyLong(), anyMap(), any(Locale.class)))
                .thenThrow(new IllegalStateException("render failed"));

        Plan updated = planService.startNode(plan.getId(), plan.getNodes().get(0).getId(), "operator-2");

        verify(notificationGateway, times(0)).sendEmail(any(EmailMessage.class));
        assertThat(actionHistoryRepository.entries).hasSize(1);
        PlanActionHistory history = actionHistoryRepository.entries.get(0);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.FAILED);
        assertThat(history.getError()).isEqualTo("render failed");
        assertThat(history.getMetadata()).containsEntry("reason", "EXCEPTION");

        PlanActivity actionActivity = findActionActivity(updated);
        assertThat(actionActivity.getAttributes())
                .containsEntry("actionStatus", PlanActionStatus.FAILED.name())
                .containsEntry("actionError", "render failed")
                .containsEntry("actionMessage", "plan.action.failed");
    }

    @Test
    void completeNode_shouldDispatchInstantMessageWithRetriesOnFailure() {
        Plan plan = seedPlan("plan-im-failure", PlanStatus.IN_PROGRESS, PlanNodeStatus.IN_PROGRESS,
                PlanNodeActionType.IM, "201", "carol");
        aggregateRepository.planRepository.save(plan);
        RenderedTemplate template = new RenderedTemplate(
                null,
                "IM content",
                List.of("user"),
                List.of(),
                null,
                null,
                null,
                null,
                Map.of()
        );
        when(templateService.render(anyLong(), anyMap(), any(Locale.class))).thenReturn(template);
        when(notificationGateway.sendInstantMessage(any(InstantMessage.class)))
                .thenThrow(new IllegalStateException("gateway down"));

        Plan updated = planService.completeNode(plan.getId(), plan.getNodes().get(0).getId(),
                "operator-3", "ok", null, null);

        verify(notificationGateway, times(3)).sendInstantMessage(any(InstantMessage.class));
        assertThat(actionHistoryRepository.entries).hasSize(1);
        PlanActionHistory history = actionHistoryRepository.entries.get(0);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.FAILED);
        assertThat(history.getError()).isEqualTo("gateway down");
        assertThat(history.getMetadata()).containsEntry("attempts", "3").containsEntry("reason", "EXCEPTION");

        PlanActivity actionActivity = findActionActivity(updated);
        assertThat(actionActivity.getAttributes())
                .containsEntry("actionStatus", PlanActionStatus.FAILED.name())
                .containsEntry("actionError", "gateway down")
                .containsEntry("meta.attempts", "3")
                .containsEntry("actionType", PlanNodeActionType.IM.name());
    }

    @Test
    void completeNode_shouldRecordInstantMessageSuccess() {
        Plan plan = seedPlan("plan-im-success", PlanStatus.IN_PROGRESS, PlanNodeStatus.IN_PROGRESS,
                PlanNodeActionType.IM, "202", "dave");
        aggregateRepository.planRepository.save(plan);
        RenderedTemplate template = new RenderedTemplate(
                null,
                "IM content",
                List.of("user"),
                List.of(),
                null,
                null,
                null,
                null,
                Map.of("templateMeta", "value")
        );
        when(templateService.render(anyLong(), anyMap(), any(Locale.class))).thenReturn(template);
        NotificationResult success = NotificationResult.success("IM", "im.sent", Map.of("channel", "mock"));
        when(notificationGateway.sendInstantMessage(any(InstantMessage.class))).thenReturn(success);

        Plan updated = planService.completeNode(plan.getId(), plan.getNodes().get(0).getId(),
                "operator-4", "done", null, null);

        verify(notificationGateway).sendInstantMessage(any(InstantMessage.class));
        PlanActionHistory history = actionHistoryRepository.entries.get(0);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.SUCCESS);
        assertThat(history.getMetadata()).containsEntry("templateId", "202");

        PlanActivity actionActivity = findActionActivity(updated);
        assertThat(actionActivity.getAttributes())
                .containsEntry("actionStatus", PlanActionStatus.SUCCESS.name())
                .containsEntry("actionMessage", "im.sent")
                .containsEntry("meta.channel", "mock");
    }

    @Test
    void handoverNode_shouldDispatchRemoteActionAndRecordSuccess() {
        Plan plan = seedPlan("plan-remote-success", PlanStatus.IN_PROGRESS, PlanNodeStatus.PENDING,
                PlanNodeActionType.REMOTE, "301", "erin");
        aggregateRepository.planRepository.save(plan);
        RenderedTemplate template = new RenderedTemplate(
                null,
                null,
                List.of(),
                List.of(),
                "https://remote.example.com/session",
                "artifact.txt",
                null,
                null,
                Map.of("token", UUID.randomUUID().toString())
        );
        when(templateService.render(anyLong(), anyMap(), any(Locale.class))).thenReturn(template);

        Plan updated = planService.handoverNode(plan.getId(), plan.getNodes().get(0).getId(),
                "frank", "please join", "operator-5");

        assertThat(actionHistoryRepository.entries).hasSize(1);
        PlanActionHistory history = actionHistoryRepository.entries.get(0);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.SUCCESS);
        assertThat(history.getMetadata()).containsEntry("endpoint", "https://remote.example.com/session");

        PlanActivity actionActivity = findActionActivity(updated);
        assertThat(actionActivity.getAttributes())
                .containsEntry("actionStatus", PlanActionStatus.SUCCESS.name())
                .containsEntry("meta.endpoint", "https://remote.example.com/session")
                .containsEntry("context.nodeAssignee", "frank")
                .containsEntry("context.trigger", "handover");
    }

    @Test
    void handoverNode_shouldRecordRemoteActionFailureWhenTemplateFails() {
        Plan plan = seedPlan("plan-remote-failure", PlanStatus.IN_PROGRESS, PlanNodeStatus.PENDING,
                PlanNodeActionType.REMOTE, "302", "gina");
        aggregateRepository.planRepository.save(plan);
        when(templateService.render(anyLong(), anyMap(), any(Locale.class)))
                .thenThrow(new IllegalArgumentException("template missing"));

        Plan updated = planService.handoverNode(plan.getId(), plan.getNodes().get(0).getId(),
                "hank", "handover comment", "operator-6");

        assertThat(actionHistoryRepository.entries).hasSize(1);
        PlanActionHistory history = actionHistoryRepository.entries.get(0);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.FAILED);
        assertThat(history.getError()).isEqualTo("template missing");
        assertThat(history.getContext()).containsEntry("trigger", "handover");

        PlanActivity actionActivity = findActionActivity(updated);
        assertThat(actionActivity.getAttributes())
                .containsEntry("actionStatus", PlanActionStatus.FAILED.name())
                .containsEntry("actionError", "template missing")
                .containsEntry("context.trigger", "handover")
                .containsEntry("result", "handover comment");
    }

    private Plan seedPlan(String planId, PlanStatus planStatus, PlanNodeStatus executionStatus,
                          PlanNodeActionType actionType, String actionRef, String assignee) {
        PlanNode node = new PlanNode(
                "node-" + planId,
                "Node " + planId,
                "TASK",
                assignee,
                1,
                null,
                actionType,
                100,
                actionRef,
                null,
                List.of()
        );
        PlanNodeExecution execution = new PlanNodeExecution(
                node.getId(),
                executionStatus,
                executionStatus == PlanNodeStatus.IN_PROGRESS ? OffsetDateTime.now().minusMinutes(5) : null,
                null,
                null,
                null,
                null,
                List.of()
        );
        return new Plan(
                planId,
                "tenant-1",
                "Plan " + planId,
                "desc",
                "customer-1",
                "owner-1",
                List.of("owner-1"),
                planStatus,
                OffsetDateTime.now().minusHours(1),
                OffsetDateTime.now().plusHours(2),
                planStatus == PlanStatus.IN_PROGRESS ? OffsetDateTime.now().minusHours(1) : null,
                null,
                null,
                null,
                null,
                "UTC",
                List.of(node),
                List.of(execution),
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().minusDays(1),
                List.of(),
                PlanReminderPolicy.empty()
        );
    }

    private PlanActivity findActionActivity(Plan plan) {
        return plan.getActivities().stream()
                .filter(activity -> activity.getType() == PlanActivityType.NODE_ACTION_EXECUTED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing NODE_ACTION_EXECUTED activity"));
    }

    private static final class RecordingPlanActionHistoryRepository implements PlanActionHistoryRepository {

        private final List<PlanActionHistory> entries = new ArrayList<>();

        @Override
        public void append(PlanActionHistory history) {
            entries.add(history);
        }

        @Override
        public List<PlanActionHistory> findByPlanId(String planId) {
            return entries.stream().filter(history -> history.getPlanId().equals(planId)).toList();
        }

        @Override
        public void deleteByPlanId(String planId) {
            entries.removeIf(history -> history.getPlanId().equals(planId));
        }
    }

    private static final class StubPlanAggregateRepository implements PlanAggregateRepository {

        private final StubPlanRepository planRepository = new StubPlanRepository();
        private final StubPlanReminderPolicyRepository reminderRepository = new StubPlanReminderPolicyRepository();
        private final StubPlanTimelineRepository timelineRepository = new StubPlanTimelineRepository();
        private final StubPlanAttachmentRepository attachmentRepository = new StubPlanAttachmentRepository();

        @Override
        public PlanRepository plans() {
            return planRepository;
        }

        @Override
        public PlanReminderPolicyRepository reminderPolicies() {
            return reminderRepository;
        }

        @Override
        public PlanTimelineRepository timelines() {
            return timelineRepository;
        }

        @Override
        public PlanAttachmentRepository attachments() {
            return attachmentRepository;
        }
    }

    private static final class StubPlanRepository implements PlanRepository {

        private final Map<String, Plan> storage = new LinkedHashMap<>();
        private int planSeq = 1;
        private int nodeSeq = 1;
        private int reminderSeq = 1;

        @Override
        public List<Plan> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public List<Plan> findByCriteria(PlanSearchCriteria criteria) {
            return findAll();
        }

        @Override
        public int countByCriteria(PlanSearchCriteria criteria) {
            return storage.size();
        }

        @Override
        public Optional<Plan> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public void save(Plan plan) {
            storage.put(plan.getId(), plan);
        }

        @Override
        public void delete(String id) {
            storage.remove(id);
        }

        @Override
        public String nextPlanId() {
            return "plan-" + planSeq++;
        }

        @Override
        public String nextNodeId() {
            return "node-" + nodeSeq++;
        }

        @Override
        public String nextReminderId() {
            return "reminder-" + reminderSeq++;
        }
    }

    private static final class StubPlanReminderPolicyRepository implements PlanReminderPolicyRepository {

        private final Map<String, PlanReminderPolicy> storage = new HashMap<>();

        @Override
        public Optional<PlanReminderPolicy> findReminderPolicy(String planId) {
            return Optional.ofNullable(storage.get(planId));
        }

        @Override
        public void replaceReminderPolicy(String planId, PlanReminderPolicy policy) {
            storage.put(planId, policy);
        }
    }

    private static final class StubPlanTimelineRepository implements PlanTimelineRepository {

        private final Map<String, List<PlanActivity>> storage = new HashMap<>();

        @Override
        public List<PlanActivity> findTimeline(String planId) {
            return storage.getOrDefault(planId, List.of());
        }

        @Override
        public void replaceTimeline(String planId, List<PlanActivity> activities) {
            storage.put(planId, List.copyOf(activities));
        }
    }

    private static final class StubPlanAttachmentRepository implements PlanAttachmentRepository {

        private final Map<String, Map<String, List<String>>> storage = new HashMap<>();

        @Override
        public Map<String, List<String>> findAttachments(String planId) {
            return storage.getOrDefault(planId, Map.of());
        }

        @Override
        public void replaceAttachments(String planId, Map<String, List<String>> attachments) {
            storage.put(planId, Map.copyOf(attachments));
        }
    }

    private static final class NoopPlanAnalyticsRepository implements PlanAnalyticsRepository {

        @Override
        public PlanAnalytics summarize(PlanAnalyticsQuery query) {
            return null;
        }

        @Override
        public com.bob.mta.modules.plan.service.PlanBoardView getPlanBoard(String tenantId, PlanBoardWindow window,
                                                                           PlanBoardGrouping grouping) {
            return null;
        }
    }

    private static final class NoopFileService implements FileService {

        @Override
        public FileMetadata register(String fileName, String contentType, long size, String bucket, String bizType,
                                     String bizId, String uploader) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileMetadata get(String id) {
            return null;
        }

        @Override
        public List<FileMetadata> listByBiz(String bizType, String bizId) {
            return List.of();
        }

        @Override
        public void delete(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String buildDownloadUrl(FileMetadata metadata) {
            throw new UnsupportedOperationException();
        }
    }
}

