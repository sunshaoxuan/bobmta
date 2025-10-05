package com.bob.mta.modules.plan.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.audit.domain.AuditLog;
import com.bob.mta.modules.audit.service.AuditQuery;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.bob.mta.modules.plan.domain.PlanActionStatus;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.dto.PlanActionHistoryResponse;
import com.bob.mta.modules.plan.dto.CancelPlanRequest;
import com.bob.mta.modules.plan.dto.CompleteNodeRequest;
import com.bob.mta.modules.plan.dto.PlanActivityResponse;
import com.bob.mta.modules.plan.dto.PlanBoardResponse;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanFilterOptionsResponse;
import com.bob.mta.modules.plan.dto.PlanHandoverRequest;
import com.bob.mta.modules.plan.dto.PlanNodeAttachmentResponse;
import com.bob.mta.modules.plan.dto.PlanNodeHandoverRequest;
import com.bob.mta.modules.plan.dto.PlanNodeStartRequest;
import com.bob.mta.modules.plan.dto.PlanReminderPolicyRequest;
import com.bob.mta.modules.plan.dto.PlanReminderRuleRequest;
import com.bob.mta.modules.plan.dto.PlanReminderUpdateRequest;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.repository.InMemoryPlanActionHistoryRepository;
import com.bob.mta.modules.plan.repository.InMemoryPlanAnalyticsRepository;
import com.bob.mta.modules.plan.repository.InMemoryPlanRepository;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;
import com.bob.mta.modules.plan.service.PlanBoardView;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.plan.service.impl.InMemoryPlanService;
import com.bob.mta.modules.plan.service.impl.RecordingNotificationGateway;
import com.bob.mta.modules.plan.service.impl.TestTemplateService;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.template.domain.RenderedTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanControllerTest {

    private PlanController controller;
    private InMemoryPlanService planService;
    private InMemoryPlanRepository planRepository;
    private InMemoryFileService fileService;
    private InMemoryAuditService auditService;
    private MessageResolver messageResolver;
    private InMemoryPlanActionHistoryRepository actionHistoryRepository;
    private TestTemplateService templateService;
    private RecordingNotificationGateway notificationGateway;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        fileService = new InMemoryFileService();
        planRepository = new InMemoryPlanRepository();
        messageResolver = TestMessageResolverFactory.create();
        actionHistoryRepository = new InMemoryPlanActionHistoryRepository();
        templateService = new TestTemplateService();
        notificationGateway = new RecordingNotificationGateway();
        planService = new InMemoryPlanService(fileService, planRepository,
                new InMemoryPlanAnalyticsRepository(planRepository), actionHistoryRepository,
                templateService, notificationGateway, notificationGateway, notificationGateway, messageResolver);
        auditService = new InMemoryAuditService();
        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        controller = new PlanController(planService, recorder, fileService, messageResolver);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void listShouldReturnPlans() {
        PageResponse<PlanSummaryResponse> page = controller.list(null, null, null, null, null, null, null, 0, 1)
                .getData();
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void listShouldFilterByOwnerAndKeyword() {
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-002",
                "数据中心深度巡检",
                "针对苏州数据中心的全面巡检",
                "cust-003",
                "ops-lead",
                OffsetDateTime.now().plusDays(5),
                OffsetDateTime.now().plusDays(5).plusHours(3),
                "Asia/Shanghai",
                List.of("ops-lead"),
                List.of(new PlanNodeCommand(null, "巡检准备", "CHECKLIST", "ops-lead", 1, 60, PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        planService.createPlan(command);

        PageResponse<PlanSummaryResponse> filtered = controller
                .list(null, null, "ops-lead", "数据中心", null, null, null, 0, 10)
                .getData();

        assertThat(filtered.getItems()).hasSize(1);
        assertThat(filtered.getItems().get(0).getOwner()).isEqualTo("ops-lead");
    }

    @Test
    void listShouldRespectTenantBoundary() {
        CreatePlanCommand tenantSpecific = new CreatePlanCommand(
                "tenant-isolated",
                "海外机房季度体检",
                "tenant-isolated-only",
                "cust-777",
                "tenant-owner",
                OffsetDateTime.now().plusDays(3),
                OffsetDateTime.now().plusDays(3).plusHours(6),
                "Asia/Tokyo",
                List.of("tenant-owner"),
                List.of(new PlanNodeCommand(null, "准备", "CHECKLIST", "tenant-owner", 1, 60, PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        planService.createPlan(tenantSpecific);

        PageResponse<PlanSummaryResponse> isolated = controller
                .list("tenant-isolated", null, null, null, null, null, null, 0, 10)
                .getData();

        assertThat(isolated.getItems()).extracting(PlanSummaryResponse::getTenantId)
                .containsOnly("tenant-isolated");

        PageResponse<PlanSummaryResponse> defaultTenant = controller
                .list("tenant-001", null, null, null, null, null, null, 0, 10)
                .getData();

        assertThat(defaultTenant.getItems())
                .allMatch(summary -> "tenant-001".equals(summary.getTenantId()));
    }

    @Test
    void analyticsShouldSummarizePlans() {
        var analytics = controller.analytics(null, null, null, null, null).getData();

        assertThat(analytics.getTotalPlans()).isGreaterThanOrEqualTo(2);
        assertThat(analytics.getUpcomingPlans()).isNotEmpty();
        assertThat(analytics.getOwnerLoads()).isNotEmpty();
        assertThat(analytics.getRiskPlans()).isNotNull();
    }

    @Test
    void analyticsShouldFilterByOwner() {
        OffsetDateTime plannedStart = OffsetDateTime.now().plusDays(2);
        CreatePlanCommand focusCommand = new CreatePlanCommand(
                "tenant-owner-controller",
                "Owner focused plan",
                "Controller scoped owner",
                "cust-ctrl",
                "controller-owner",
                plannedStart,
                plannedStart.plusHours(2),
                "Asia/Tokyo",
                List.of("controller-owner"),
                List.of(new PlanNodeCommand(null, "准备巡检", "CHECKLIST", "controller-owner", 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var focusPlan = planService.createPlan(focusCommand);
        planService.publishPlan(focusPlan.getId(), "controller-owner");

        CreatePlanCommand otherOwner = new CreatePlanCommand(
                "tenant-owner-controller",
                "Other owner coverage",
                "Different owner", 
                "cust-ctrl",
                "controller-other",
                plannedStart.plusDays(1),
                plannedStart.plusDays(1).plusHours(3),
                "Asia/Tokyo",
                List.of("controller-other"),
                List.of(new PlanNodeCommand(null, "执行检查", "CHECKLIST", "controller-other", 1, 45,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var otherPlan = planService.createPlan(otherOwner);
        planService.publishPlan(otherPlan.getId(), "controller-other");

        var analytics = controller.analytics("tenant-owner-controller", null, "controller-owner", null, null)
                .getData();

        assertThat(analytics.getOwnerLoads()).hasSize(1);
        assertThat(analytics.getOwnerLoads().get(0).getOwnerId()).isEqualTo("controller-owner");
        assertThat(analytics.getUpcomingPlans())
                .allSatisfy(plan -> assertThat(plan.getOwner()).isEqualTo("controller-owner"));
    }

    @Test
    void boardShouldReturnAggregatedView() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        CreatePlanCommand first = new CreatePlanCommand(
                "tenant-controller-board",
                "控制层看板计划A",
                "board view sample",
                "cust-board-1",
                "controller-board-owner",
                start,
                start.plusHours(3),
                "Asia/Shanghai",
                List.of("controller-board-owner"),
                List.of(new PlanNodeCommand(null, "准备阶段", "CHECKLIST", "controller-board-owner", 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var firstPlan = planService.createPlan(first);
        planService.publishPlan(firstPlan.getId(), "controller-board-owner");

        CreatePlanCommand second = new CreatePlanCommand(
                "tenant-controller-board",
                "控制层看板计划B",
                "board view sample",
                "cust-board-2",
                "controller-board-owner",
                start.plusDays(1),
                start.plusDays(1).plusHours(2),
                "Asia/Shanghai",
                List.of("controller-board-owner"),
                List.of(new PlanNodeCommand(null, "执行阶段", "CHECKLIST", "controller-board-owner", 1, 45,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var secondPlan = planService.createPlan(second);
        planService.publishPlan(secondPlan.getId(), "controller-board-owner");

        ApiResponse<PlanBoardResponse> response = controller.board(
                "tenant-controller-board",
                List.of("cust-board-1", "cust-board-2"),
                null,
                List.of(PlanStatus.SCHEDULED),
                null,
                null,
                PlanBoardGrouping.DAY);

        PlanBoardResponse board = response.getData();
        assertThat(board.getMetrics().getTotalPlans()).isEqualTo(2);
        assertThat(board.getCustomerGroups()).hasSize(2);
        assertThat(board.getTimeBuckets()).isNotEmpty();
        assertThat(board.getTimeBuckets().get(0).getPlans())
                .extracting(PlanBoardResponse.PlanCardResponse::getCustomerId)
                .containsAnyOf("cust-board-1", "cust-board-2");
        assertThat(board.getMetrics().getDueSoonPlans()).isGreaterThanOrEqualTo(0);
        assertThat(board.getMetrics().getAtRiskPlans()).isGreaterThanOrEqualTo(0);
        assertThat(board.getCustomerGroups())
                .allSatisfy(group -> assertThat(group.getAtRiskPlans())
                        .isEqualTo(group.getOverduePlans() + group.getDueSoonPlans()));
        assertThat(board.getTimeBuckets())
                .allSatisfy(bucket -> assertThat(bucket.getAtRiskPlans())
                        .isEqualTo(bucket.getOverduePlans() + bucket.getDueSoonPlans()));

        List<AuditLog> logs = auditService.query(new AuditQuery(
                "PlanBoard", "tenant-controller-board", "VIEW_PLAN_BOARD", null));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getNewData()).contains("cust-board-1");
    }

    @Test
    void boardShouldSanitizeFiltersBeforeDelegating() {
        PlanService planServiceMock = Mockito.mock(PlanService.class);
        OffsetDateTime reference = OffsetDateTime.parse("2024-06-05T00:00:00Z");
        PlanBoardView emptyView = new PlanBoardView(List.of(), List.of(),
                new PlanBoardView.Metrics(0, 0, 0, 0, 0, 0, 0, 0, 0), PlanBoardGrouping.WEEK, reference);
        when(planServiceMock.getPlanBoard(any(), any())).thenReturn(emptyView);
        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        PlanController sanitizedController = new PlanController(planServiceMock, recorder, fileService, messageResolver);

        ApiResponse<PlanBoardResponse> response = sanitizedController.board(
                "tenant-sanitize",
                List.of(" cust-a ", "", null, "cust-b", "cust-a"),
                "owner-sanitize",
                List.of(PlanStatus.SCHEDULED, null, PlanStatus.SCHEDULED, PlanStatus.COMPLETED),
                null,
                null,
                PlanBoardGrouping.MONTH);

        assertThat(response.getData()).isNotNull();

        ArgumentCaptor<PlanSearchCriteria> criteriaCaptor = ArgumentCaptor.forClass(PlanSearchCriteria.class);
        ArgumentCaptor<PlanBoardGrouping> groupingCaptor = ArgumentCaptor.forClass(PlanBoardGrouping.class);
        verify(planServiceMock).getPlanBoard(criteriaCaptor.capture(), groupingCaptor.capture());

        PlanSearchCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.getTenantId()).isEqualTo("tenant-sanitize");
        assertThat(criteria.getOwner()).isEqualTo("owner-sanitize");
        assertThat(criteria.getCustomerIds()).containsExactly("cust-a", "cust-b");
        assertThat(criteria.getStatuses()).containsExactly(PlanStatus.SCHEDULED, PlanStatus.COMPLETED);
        assertThat(groupingCaptor.getValue()).isEqualTo(PlanBoardGrouping.MONTH);
    }

    @Test
    void boardShouldReturnZeroMetricsWhenServiceOmitsAggregates() {
        PlanService planServiceMock = Mockito.mock(PlanService.class);
        OffsetDateTime reference = OffsetDateTime.parse("2024-06-12T00:00:00Z");
        PlanBoardView viewWithoutMetrics = new PlanBoardView(List.of(), List.of(), null,
                PlanBoardGrouping.DAY, reference);
        when(planServiceMock.getPlanBoard(any(), any())).thenReturn(viewWithoutMetrics);

        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        PlanController controllerWithMock = new PlanController(planServiceMock, recorder, fileService, messageResolver);

        ApiResponse<PlanBoardResponse> response = controllerWithMock.board(
                "tenant-zero-metrics",
                null,
                null,
                null,
                null,
                null,
                PlanBoardGrouping.DAY);

        PlanBoardResponse board = response.getData();
        assertThat(board.getMetrics().getTotalPlans()).isZero();
        assertThat(board.getMetrics().getDueSoonPlans()).isZero();
        assertThat(board.getMetrics().getAtRiskPlans()).isZero();
        assertThat(board.getReferenceTime()).isEqualTo(reference);
        verify(planServiceMock).getPlanBoard(any(), eq(PlanBoardGrouping.DAY));
    }

    @Test
    void boardShouldTreatBlankTenantIdAsGlobalScope() {
        PlanService planServiceMock = Mockito.mock(PlanService.class);
        OffsetDateTime reference = OffsetDateTime.parse("2024-06-10T00:00:00Z");
        PlanBoardView emptyView = new PlanBoardView(List.of(), List.of(), null, PlanBoardGrouping.MONTH, reference);
        when(planServiceMock.getPlanBoard(any(), any())).thenReturn(emptyView);

        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        PlanController blankTenantController = new PlanController(planServiceMock, recorder, fileService, messageResolver);

        ApiResponse<PlanBoardResponse> response = blankTenantController.board(
                "   ",
                List.of("cust-blank"),
                "owner-blank",
                null,
                null,
                null,
                PlanBoardGrouping.MONTH);

        assertThat(response.getData()).isNotNull();

        ArgumentCaptor<PlanSearchCriteria> criteriaCaptor = ArgumentCaptor.forClass(PlanSearchCriteria.class);
        verify(planServiceMock).getPlanBoard(criteriaCaptor.capture(), eq(PlanBoardGrouping.MONTH));

        PlanSearchCriteria delegatedCriteria = criteriaCaptor.getValue();
        assertThat(delegatedCriteria.getTenantId()).isNull();
        assertThat(delegatedCriteria.getCustomerIds()).containsExactly("cust-blank");
    }

    @Test
    void boardShouldRecordGlobalAuditSnapshotWhenTenantMissing() {
        PlanService planServiceMock = Mockito.mock(PlanService.class);
        OffsetDateTime reference = OffsetDateTime.parse("2024-07-10T00:00:00Z");
        PlanBoardView boardView = new PlanBoardView(List.of(), List.of(),
                new PlanBoardView.Metrics(0, 0, 0, 0, 0, 0, 0, 0, 0), PlanBoardGrouping.MONTH, reference);
        when(planServiceMock.getPlanBoard(any(), any())).thenReturn(boardView);

        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        PlanController globalScopeController = new PlanController(planServiceMock, recorder, fileService, messageResolver);

        globalScopeController.board(
                "   ",
                null,
                null,
                null,
                null,
                null,
                PlanBoardGrouping.MONTH);

        List<AuditLog> logs = auditService.query(new AuditQuery("PlanBoard", "GLOBAL", "VIEW_PLAN_BOARD", null));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getNewData()).contains("\"granularity\":\"MONTH\"");
    }

    @Test
    void boardShouldDefaultGranularityToWeekWhenNull() {
        PlanService planServiceMock = Mockito.mock(PlanService.class);
        OffsetDateTime reference = OffsetDateTime.parse("2024-06-12T00:00:00Z");
        PlanBoardView emptyView = new PlanBoardView(List.of(), List.of(), null, PlanBoardGrouping.WEEK, reference);
        when(planServiceMock.getPlanBoard(any(), any())).thenReturn(emptyView);

        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        PlanController defaultGroupingController = new PlanController(planServiceMock, recorder, fileService, messageResolver);

        ApiResponse<PlanBoardResponse> response = defaultGroupingController.board(
                "tenant-default-week",
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(response.getData()).isNotNull();

        ArgumentCaptor<PlanBoardGrouping> groupingCaptor = ArgumentCaptor.forClass(PlanBoardGrouping.class);
        verify(planServiceMock).getPlanBoard(any(), groupingCaptor.capture());
        assertThat(groupingCaptor.getValue()).isEqualTo(PlanBoardGrouping.WEEK);
    }

    @Test
    void boardShouldReturnZeroMetricsWhenRepositoryOmitsAggregates() {
        PlanService planServiceMock = Mockito.mock(PlanService.class);
        OffsetDateTime reference = OffsetDateTime.parse("2024-07-01T00:00:00Z");
        PlanBoardView viewWithoutMetrics = new PlanBoardView(List.of(), List.of(), null, PlanBoardGrouping.WEEK, reference);
        when(planServiceMock.getPlanBoard(any(), any())).thenReturn(viewWithoutMetrics);

        AuditRecorder recorder = new AuditRecorder(auditService, new ObjectMapper());
        PlanController controllerWithMock = new PlanController(planServiceMock, recorder, fileService, messageResolver);

        ApiResponse<PlanBoardResponse> response = controllerWithMock.board(
                null,
                null,
                null,
                null,
                null,
                null,
                PlanBoardGrouping.WEEK);

        assertThat(response.getData().getMetrics().getTotalPlans()).isZero();
        assertThat(response.getData().getMetrics().getCompletionRate()).isZero();
        assertThat(response.getData().getReferenceTime()).isEqualTo(reference);

        verify(planServiceMock).getPlanBoard(any(), eq(PlanBoardGrouping.WEEK));
    }

    @Test
    @DisplayName("board should filter by tenant scope and sort plan cards")
    void boardShouldFilterByTenantScopeAndSortPlans() {
        OffsetDateTime base = OffsetDateTime.parse("2024-09-01T08:00:00+08:00");
        CreatePlanCommand laterStart = new CreatePlanCommand(
                "tenant-controller-sorting",
                "驾驶舱排序-较晚计划",
                "同一客户稍晚开始的计划",
                "cust-board-sort",
                "controller-board-owner",
                base.plusHours(4),
                base.plusHours(6),
                "Asia/Shanghai",
                List.of("controller-board-owner"),
                List.of(new PlanNodeCommand(null, "准备阶段", "CHECKLIST", "controller-board-owner", 1, 45,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        CreatePlanCommand earlierStart = new CreatePlanCommand(
                "tenant-controller-sorting",
                "驾驶舱排序-最早计划",
                "同一客户较早开始的计划",
                "cust-board-sort",
                "controller-board-owner",
                base.plusHours(1),
                base.plusHours(2),
                "Asia/Shanghai",
                List.of("controller-board-owner"),
                List.of(new PlanNodeCommand(null, "执行阶段", "CHECKLIST", "controller-board-owner", 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        CreatePlanCommand otherTenant = new CreatePlanCommand(
                "tenant-controller-sorting-other",
                "驾驶舱排序-其他租户",
                "不同租户的计划不应出现",
                "cust-board-sort",
                "controller-board-owner",
                base.plusHours(3),
                base.plusHours(5),
                "Asia/Shanghai",
                List.of("controller-board-owner"),
                List.of(new PlanNodeCommand(null, "忽略阶段", "CHECKLIST", "controller-board-owner", 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );

        var laterPlan = planService.createPlan(laterStart);
        planService.publishPlan(laterPlan.getId(), "controller-board-owner");
        var earlierPlan = planService.createPlan(earlierStart);
        planService.publishPlan(earlierPlan.getId(), "controller-board-owner");
        var otherPlan = planService.createPlan(otherTenant);
        planService.publishPlan(otherPlan.getId(), "controller-board-owner");

        ApiResponse<PlanBoardResponse> response = controller.board(
                "tenant-controller-sorting",
                null,
                null,
                null,
                base.minusDays(1),
                base.plusDays(1),
                PlanBoardGrouping.DAY);

        PlanBoardResponse board = response.getData();
        assertThat(board.getMetrics().getTotalPlans()).isEqualTo(2);
        assertThat(board.getCustomerGroups()).hasSize(1);
        PlanBoardResponse.CustomerGroupResponse group = board.getCustomerGroups().get(0);
        assertThat(group.getCustomerId()).isEqualTo("cust-board-sort");
        assertThat(group.getPlans()).extracting(PlanBoardResponse.PlanCardResponse::getPlannedStartTime)
                .containsExactly(base.plusHours(1), base.plusHours(4));
        assertThat(group.getPlans())
                .extracting(PlanBoardResponse.PlanCardResponse::getId)
                .containsExactly(earlierPlan.getId(), laterPlan.getId())
                .doesNotContain(otherPlan.getId());

        assertThat(board.getTimeBuckets()).hasSize(1);
        PlanBoardResponse.TimeBucketResponse bucket = board.getTimeBuckets().get(0);
        assertThat(bucket.getPlans())
                .extracting(PlanBoardResponse.PlanCardResponse::getId)
                .containsExactly(earlierPlan.getId(), laterPlan.getId());
    }

    @Test
    @DisplayName("board should order customer groups by totals and id when tied")
    void boardShouldOrderCustomerGroupsByTotals() {
        OffsetDateTime base = OffsetDateTime.parse("2024-10-12T02:00:00+08:00");

        var alphaOne = planService.createPlan(boardPlan(
                "tenant-board-order",
                "客户A-计划1",
                "cust-alpha",
                "owner-order",
                base.plusHours(1),
                base.plusHours(3)));
        var alphaTwo = planService.createPlan(boardPlan(
                "tenant-board-order",
                "客户A-计划2",
                "cust-alpha",
                "owner-order",
                base.plusHours(6),
                base.plusHours(8)));
        var alphaThree = planService.createPlan(boardPlan(
                "tenant-board-order",
                "客户A-计划3",
                "cust-alpha",
                "owner-order",
                base.plusDays(1),
                base.plusDays(1).plusHours(2)));

        var betaOne = planService.createPlan(boardPlan(
                "tenant-board-order",
                "客户B-计划1",
                "cust-beta",
                "owner-order",
                base.plusDays(1).plusHours(4),
                base.plusDays(1).plusHours(6)));
        var betaTwo = planService.createPlan(boardPlan(
                "tenant-board-order",
                "客户B-计划2",
                "cust-beta",
                "owner-order",
                base.plusDays(2),
                base.plusDays(2).plusHours(2)));

        var zetaOne = planService.createPlan(boardPlan(
                "tenant-board-order",
                "客户Z-计划1",
                "cust-zeta",
                "owner-order",
                base.plusDays(2).plusHours(3),
                base.plusDays(2).plusHours(5)));
        var zetaTwo = planService.createPlan(boardPlan(
                "tenant-board-order",
                "客户Z-计划2",
                "cust-zeta",
                "owner-order",
                base.plusDays(3),
                base.plusDays(3).plusHours(2)));

        var otherTenant = planService.createPlan(boardPlan(
                "tenant-board-other",
                "其他租户计划",
                "cust-alpha",
                "owner-order",
                base.plusHours(2),
                base.plusHours(4)));

        List.of(alphaOne, alphaTwo, alphaThree, betaOne, betaTwo, zetaOne, zetaTwo, otherTenant)
                .forEach(plan -> planService.publishPlan(plan.getId(), "owner-order"));

        ApiResponse<PlanBoardResponse> response = controller.board(
                "tenant-board-order",
                null,
                null,
                null,
                base.minusDays(1),
                base.plusDays(5),
                PlanBoardGrouping.DAY);

        PlanBoardResponse board = response.getData();
        assertThat(board.getMetrics().getTotalPlans()).isEqualTo(7);
        assertThat(board.getCustomerGroups())
                .extracting(PlanBoardResponse.CustomerGroupResponse::getCustomerId)
                .containsExactly("cust-alpha", "cust-beta", "cust-zeta");
        assertThat(board.getCustomerGroups())
                .extracting(PlanBoardResponse.CustomerGroupResponse::getTotalPlans)
                .containsExactly(3L, 2L, 2L);
        assertThat(board.getTimeBuckets())
                .extracting(PlanBoardResponse.TimeBucketResponse::getStart)
                .isSorted();
    }

    @Test
    void boardShouldExposeUnknownCustomerGroup() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-controller-unknown",
                "未知客户计划",
                "board view unknown",
                null,
                "controller-unknown-owner",
                start,
                start.plusHours(2),
                "Asia/Shanghai",
                List.of("controller-unknown-owner"),
                List.of(new PlanNodeCommand(null, "未知节点", "CHECKLIST", "controller-unknown-owner", 1, 60,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var plan = planService.createPlan(command);
        planService.publishPlan(plan.getId(), "controller-unknown-owner");

        ApiResponse<PlanBoardResponse> response = controller.board(
                "tenant-controller-unknown",
                null,
                null,
                null,
                null,
                null,
                PlanBoardGrouping.DAY);

        PlanBoardResponse board = response.getData();
        assertThat(board.getCustomerGroups())
                .extracting(PlanBoardResponse.CustomerGroupResponse::getCustomerId)
                .containsExactly(PlanBoardView.UNKNOWN_CUSTOMER_ID);
        assertThat(board.getCustomerGroups().get(0).getPlans())
                .hasSize(1)
                .allSatisfy(card -> assertThat(card.getCustomerId()).isNull());
    }

    @Test
    void boardShouldExcludeBucketsForPlansWithoutStart() {
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-controller-null-start",
                "驾驶舱缺少开始时间",
                "计划缺少开始时间",
                "cust-controller-null-start",
                "controller-null-start-owner",
                null,
                now.plusHours(6),
                "Asia/Shanghai",
                List.of("controller-null-start-owner"),
                List.of(new PlanNodeCommand(null, "无开始节点", "CHECKLIST", "controller-null-start-owner", 1, 45,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );

        var plan = planService.createPlan(command);
        planService.publishPlan(plan.getId(), "controller-null-start-owner");

        ApiResponse<PlanBoardResponse> response = controller.board(
                "tenant-controller-null-start",
                null,
                null,
                null,
                now.minusDays(1),
                null,
                PlanBoardGrouping.DAY);

        PlanBoardResponse board = response.getData();
        assertThat(board.getMetrics().getTotalPlans()).isEqualTo(1);
        assertThat(board.getMetrics().getActivePlans()).isEqualTo(1);
        assertThat(board.getCustomerGroups())
                .singleElement()
                .satisfies(group -> {
                    assertThat(group.getCustomerId()).isEqualTo("cust-controller-null-start");
                    assertThat(group.getPlans())
                            .singleElement()
                            .satisfies(card -> {
                                assertThat(card.getPlannedStartTime()).isNull();
                                assertThat(card.getPlannedEndTime()).isEqualTo(now.plusHours(6));
                            });
                });
        assertThat(board.getTimeBuckets()).isEmpty();
    }

    @Test
    void filterOptionsShouldExposeDictionaryMetadata() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime end = start.plusHours(4);
        planService.createPlan(new CreatePlanCommand(
                "tenant-888",
                "冬季巡检",
                "冬季专项巡检计划",
                "cust-888",
                "winter-lead",
                start,
                end,
                "Asia/Tokyo",
                List.of("winter-lead", "observer"),
                List.of(new PlanNodeCommand(null, "巡检准备", "CHECKLIST", "winter-lead", 1, 120,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        ));

        var options = controller.filterOptions(null).getData();

        assertThat(options.getStatuses())
                .anySatisfy(option -> {
                    if ("DESIGN".equals(option.getValue())) {
                        assertThat(option.getLabel()).contains("设计");
                        assertThat(option.getCount()).isGreaterThanOrEqualTo(1);
                    }
                });
        assertThat(options.getOwners())
                .extracting(PlanFilterOptionsResponse.Option::getValue)
                .contains("winter-lead");
        assertThat(options.getCustomers())
                .extracting(PlanFilterOptionsResponse.Option::getValue)
                .contains("cust-888");
        assertThat(options.getPlannedWindow()).isNotNull();
        assertThat(options.getPlannedWindow().getLabel()).isNotBlank();
        assertThat(options.getPlannedWindow().getStart()).isNotNull();
        assertThat(options.getPlannedWindow().getEnd()).isNotNull();
    }

    @Test
    void activityTypesShouldExposeDictionary() {
        var descriptors = controller.activityTypes().getData();

        assertThat(descriptors).isNotEmpty();
        assertThat(descriptors)
                .anySatisfy(entry -> {
                    if (entry.getType() == PlanActivityType.NODE_COMPLETED) {
                        assertThat(entry.getMessages()).isNotEmpty();
                        assertThat(entry.getAttributes())
                                .extracting(attr -> attr.getName())
                                .contains("nodeName", "result");
                    }
                });
    }

    @Test
    void analyticsShouldHighlightOverduePlans() {
        OffsetDateTime start = OffsetDateTime.now().minusHours(2);
        OffsetDateTime end = OffsetDateTime.now().minusHours(1);
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-001",
                "过期巡检",
                "客户临时巡检",
                "cust-001",
                "admin",
                start,
                end,
                "Asia/Tokyo",
                List.of("admin"),
                List.of(new PlanNodeCommand(null, "快速检查", "CHECKLIST", "admin", 1, 15, PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var created = planService.createPlan(command);
        planService.publishPlan(created.getId(), "admin");

        var analytics = controller.analytics(null, null, null, null, null).getData();

        assertThat(analytics.getOverdueCount()).isGreaterThanOrEqualTo(1);
        assertThat(analytics.getRiskPlans())
                .anySatisfy(risk -> assertThat(risk.getRiskLevel()).isEqualTo(PlanAnalytics.RiskLevel.OVERDUE));
    }

    @Test
    void analyticsShouldSurfaceDueSoonPlans() {
        OffsetDateTime start = OffsetDateTime.now().plusHours(1);
        OffsetDateTime end = start.plusHours(2);
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-001",
                "即将到期巡检",
                "即将到期巡检描述",
                "cust-001",
                "admin",
                start,
                end,
                "Asia/Tokyo",
                List.of("admin"),
                List.of(new PlanNodeCommand(null, "到期检查", "CHECKLIST", "admin", 1, 60,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
        var created = planService.createPlan(command);
        planService.publishPlan(created.getId(), "admin");

        var analytics = controller.analytics(null, null, null, null, null).getData();

        assertThat(analytics.getRiskPlans())
                .anySatisfy(risk -> assertThat(risk.getRiskLevel()).isEqualTo(PlanAnalytics.RiskLevel.DUE_SOON));
    }

    @Test
    void detailShouldReturnPlanWithNodesAndReminders() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        PlanDetailResponse response = controller.detail(planId).getData();
        assertThat(response.getNodes()).isNotEmpty();
        assertThat(response.getTimeline()).isNotEmpty();
        assertThat(response.getReminderPolicy().getRules()).isNotEmpty();
    }

    @Test
    void detailShouldExposeNodeAttachments() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        String nodeId = planService.getPlan(planId).getExecutions().get(0).getNodeId();
        authenticate("admin");
        controller.publish(planId);
        PlanNodeStartRequest startRequest = new PlanNodeStartRequest();
        startRequest.setOperatorId("admin");
        controller.startNode(planId, nodeId, startRequest);
        var file = fileService.register("evidence.log", "text/plain", 128, "plan-files", "PLAN_NODE", nodeId,
                "admin");
        CompleteNodeRequest request = new CompleteNodeRequest();
        request.setOperatorId("admin");
        request.setResultSummary("完成");
        request.setLog("上传巡检记录");
        request.setFileIds(List.of(file.getId()));

        PlanDetailResponse response = controller.completeNode(planId, nodeId, request).getData();
        PlanNodeAttachmentResponse attachment = response.getNodes().stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElseThrow()
                .getExecution()
                .getAttachments()
                .get(0);
        assertThat(attachment.getId()).isEqualTo(file.getId());
        assertThat(attachment.getDownloadUrl()).isEqualTo(fileService.buildDownloadUrl(file));
    }

    @Test
    void reminderPolicyShouldExposeDefaultRules() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();

        var policy = controller.reminderPolicy(planId).getData();

        assertThat(policy.getRules()).isNotEmpty();
        assertThat(policy.getRules().get(0).getId()).isNotBlank();
    }

    @Test
    void updateReminderPolicyShouldRecordAuditAndPersistRules() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        authenticate("admin");
        PlanReminderRuleRequest ruleRequest = new PlanReminderRuleRequest();
        ruleRequest.setTrigger(PlanReminderTrigger.BEFORE_PLAN_START);
        ruleRequest.setOffsetMinutes(45);
        ruleRequest.setChannels(List.of("EMAIL"));
        ruleRequest.setTemplateId("custom-template");
        ruleRequest.setRecipients(List.of("OWNER"));
        ruleRequest.setDescription("开始前45分钟提醒负责人");
        PlanReminderPolicyRequest request = new PlanReminderPolicyRequest();
        request.setRules(List.of(ruleRequest));

        var response = controller.updateReminderPolicy(planId, request).getData();

        assertThat(response.getRules()).hasSize(1);
        assertThat(response.getRules().get(0).getOffsetMinutes()).isEqualTo(45);

        var logs = auditService.query(new AuditQuery("Plan", planId, "UPDATE_PLAN_REMINDERS", "admin"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getNewData()).contains("custom-template");
    }

    @Test
    void reminderOptionsShouldReturnLocalizedDictionary() {
        var options = controller.reminderOptions().getData();

        assertThat(options.getTriggers())
                .extracting(com.bob.mta.modules.plan.dto.PlanReminderOptionsResponse.Option::getId)
                .contains("BEFORE_PLAN_START", "BEFORE_PLAN_END");

        String expectedEmail = messageResolver.getMessage(LocalizationKeys.PlanReminder.CHANNEL_EMAIL);
        assertThat(options.getChannels())
                .anySatisfy(option -> {
                    if ("EMAIL".equals(option.getId())) {
                        assertThat(option.getLabel()).isEqualTo(expectedEmail);
                        assertThat(option.getDescription())
                                .isEqualTo(messageResolver.getMessage(LocalizationKeys.PlanReminder.CHANNEL_EMAIL_DESC));
                    }
                });

        assertThat(options.getMinOffsetMinutes()).isZero();
        assertThat(options.getMaxOffsetMinutes()).isEqualTo(1440);
        assertThat(options.getDefaultOffsetMinutes()).isEqualTo(60);
    }

    @Test
    void previewRemindersShouldReturnUpcomingEntries() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();

        var preview = controller.previewReminders(planId, OffsetDateTime.now().minusMinutes(1)).getData();

        assertThat(preview).isNotEmpty();
        assertThat(preview).allSatisfy(entry -> assertThat(entry.getFireTime()).isAfter(OffsetDateTime.now().minusMinutes(1)));
    }

    @Test
    void cancelShouldExposeReasonAndOperator() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        CancelPlanRequest request = new CancelPlanRequest();
        request.setReason("客户延期");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", null));

        PlanDetailResponse response = controller.cancel(planId, request).getData();

        assertThat(response.getCancelReason()).isEqualTo("客户延期");
        assertThat(response.getCanceledBy()).isEqualTo("admin");
        assertThat(response.getCanceledAt()).isNotNull();
    }

    @Test
    void handoverShouldUpdateOwnerParticipantsAndAuditTrail() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        authenticate("admin");
        PlanHandoverRequest request = new PlanHandoverRequest();
        request.setNewOwner("operator");
        request.setParticipants(List.of("operator", "observer"));
        request.setNote("夜间值班交接");

        PlanDetailResponse response = controller.handover(planId, request).getData();

        assertThat(response.getOwner()).isEqualTo("operator");
        assertThat(response.getParticipants()).containsExactlyInAnyOrder("operator", "observer");
        var logs = auditService.query(new AuditQuery("Plan", planId, "HANDOVER_PLAN", "admin"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getNewData()).contains("operator");
        var timeline = controller.timeline(planId).getData();
        assertThat(timeline)
                .extracting(entry -> entry.getType())
                .contains(PlanActivityType.PLAN_HANDOVER);
    }

    @Test
    void publishShouldRecordBeforeAndAfterInAudit() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        authenticate("admin");

        controller.publish(planId);

        var logs = auditService.query(new AuditQuery("Plan", planId, "PUBLISH_PLAN", "admin"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldData()).contains("\"status\":\"DESIGN\"");
        assertThat(logs.get(0).getNewData()).contains("\"status\":");
    }

    @Test
    void timelineShouldExposePlanActivities() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        authenticate("admin");

        controller.publish(planId);
        var timeline = controller.timeline(planId).getData();

        assertThat(timeline)
                .extracting(entry -> entry.getType())
                .contains(PlanActivityType.PLAN_CREATED, PlanActivityType.PLAN_PUBLISHED);
    }

    @Test
    void actionHistoryShouldExposeAutomationAttemptsAndAuditTrail() {
        OffsetDateTime start = OffsetDateTime.now().plusHours(1);
        CreatePlanCommand command = new CreatePlanCommand(
                "tenant-action-history",
                "动作历史计划",
                "验证动作历史查询",
                "cust-action",
                "operator",
                start,
                start.plusHours(2),
                "Asia/Shanghai",
                List.of("operator"),
                List.of(new PlanNodeCommand(null, "告警通知", "CHECKLIST", "operator", 1, 30,
                        PlanNodeActionType.EMAIL, 100, "701", "", List.of()))
        );
        var plan = planService.createPlan(command);
        planService.publishPlan(plan.getId(), "operator");
        templateService.register(701L, new RenderedTemplate(
                "通知",
                "测试内容",
                List.of("ops@example.com"),
                List.of(),
                "https://notify.example.com",
                null,
                null,
                null,
                Map.of("provider", "mock-mail")));
        authenticate("operator");
        PlanNodeStartRequest request = new PlanNodeStartRequest();
        request.setOperatorId("operator");
        controller.startNode(plan.getId(), plan.getNodes().get(0).getId(), request);

        List<PlanActionHistoryResponse> histories = controller.actionHistory(plan.getId()).getData();

        assertThat(histories).hasSize(1);
        PlanActionHistoryResponse history = histories.get(0);
        assertThat(history.getStatus()).isEqualTo(PlanActionStatus.SUCCESS);
        assertThat(history.getMetadata()).containsEntry("templateId", "701").containsEntry("provider", "mock-mail");
        assertThat(history.getContext()).containsEntry("trigger", "start").containsEntry("planId", plan.getId());

        var logs = auditService.query(new AuditQuery("PlanAction", plan.getId(), "VIEW_PLAN_ACTIONS", "operator"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getNewData()).contains("EMAIL");
    }

    @Test
    void startNodeShouldRecordStateTransitionInAudit() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        String nodeId = planService.getPlan(planId).getExecutions().get(0).getNodeId();
        authenticate("operator");
        controller.publish(planId);

        PlanNodeStartRequest request = new PlanNodeStartRequest();
        request.setOperatorId("operator");
        controller.startNode(planId, nodeId, request);

        var logs = auditService.query(new AuditQuery("PlanNode", planId + "::" + nodeId, "START_NODE", "operator"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldData()).contains("\"status\":\"PENDING\"");
        assertThat(logs.get(0).getNewData()).contains("\"status\":\"IN_PROGRESS\"");
    }

    @Test
    void completeNodeShouldRecordStateTransitionInAudit() {
        String planId = planService.listPlans(null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        String nodeId = planService.getPlan(planId).getExecutions().get(0).getNodeId();
        authenticate("operator");
        controller.publish(planId);
        PlanNodeStartRequest startRequest = new PlanNodeStartRequest();
        startRequest.setOperatorId("operator");
        controller.startNode(planId, nodeId, startRequest);
        var file = fileService.register("result.txt", "text/plain", 256, "plan-files", "PLAN_NODE", nodeId,
                "operator");
        CompleteNodeRequest request = new CompleteNodeRequest();
        request.setOperatorId("operator");
        request.setResultSummary("巡检完成");
        request.setLog("一切正常");
        request.setFileIds(List.of(file.getId()));

        PlanDetailResponse response = controller.completeNode(planId, nodeId, request).getData();

        var logs = auditService.query(new AuditQuery("PlanNode", planId + "::" + nodeId, "COMPLETE_NODE", "operator"));
        assertThat(logs).hasSize(1);
        AuditLog log = logs.get(0);
        assertThat(log.getOldData()).contains("\"status\":\"IN_PROGRESS\"");
        assertThat(log.getNewData()).contains("\"status\":\"DONE\"");
        PlanNodeAttachmentResponse attachment = response.getNodes().stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElseThrow()
                .getExecution()
                .getAttachments()
                .get(0);
        assertThat(attachment.getId()).isEqualTo(file.getId());
        assertThat(attachment.getDownloadUrl())
                .isEqualTo(fileService.buildDownloadUrl(file));
    }

    @Test
    void handoverNodeShouldUpdateAssigneeAndTimeline() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        String nodeId = planService.getPlan(planId).getExecutions().get(0).getNodeId();
        authenticate("operator");
        controller.publish(planId);

        PlanNodeHandoverRequest request = new PlanNodeHandoverRequest();
        request.setOperatorId("operator");
        request.setAssigneeId("new-operator");
        request.setComment("交接说明");

        PlanDetailResponse response = controller.handoverNode(planId, nodeId, request).getData();

        String updatedAssignee = response.getNodes().stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElseThrow()
                .getAssignee();
        assertThat(updatedAssignee).isEqualTo("new-operator");
        assertThat(response.getTimeline())
                .extracting(PlanActivityResponse::getType)
                .contains(PlanActivityType.NODE_HANDOVER);

        var logs = auditService.query(new AuditQuery("PlanNode", planId + "::" + nodeId, "HANDOVER_NODE", "operator"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getNewData()).contains("new-operator");
    }

    @Test
    void updateReminderRuleShouldToggleActiveAndOffset() {
        String planId = planService.listPlans(null, null, null, null, null, null, null, 0, 10).plans().get(0).getId();
        authenticate("admin");
        PlanDetailResponse before = controller.detail(planId).getData();
        String reminderId = before.getReminderPolicy().getRules().get(0).getId();

        PlanReminderUpdateRequest request = new PlanReminderUpdateRequest();
        request.setActive(false);
        request.setOffsetMinutes(999);

        PlanDetailResponse response = controller.updateReminderRule(planId, reminderId, request).getData();

        var updatedRule = response.getReminderPolicy().getRules().stream()
                .filter(rule -> reminderId.equals(rule.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(updatedRule.isActive()).isFalse();
        assertThat(updatedRule.getOffsetMinutes()).isEqualTo(999);
        assertThat(response.getTimeline())
                .extracting(PlanActivityResponse::getType)
                .contains(PlanActivityType.REMINDER_POLICY_UPDATED);

        var logs = auditService.query(new AuditQuery("PlanReminder", planId + "::" + reminderId,
                "UPDATE_REMINDER", "admin"));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getNewData()).contains("\"offsetMinutes\":999");
    }

    private CreatePlanCommand boardPlan(String tenantId, String title, String customerId, String owner,
                                        OffsetDateTime start, OffsetDateTime end) {
        return new CreatePlanCommand(
                tenantId,
                title,
                "board ordering scenario",
                customerId,
                owner,
                start,
                end,
                "Asia/Shanghai",
                List.of(owner),
                List.of(new PlanNodeCommand(null, "排序校验节点", "CHECKLIST", owner, 1, 30,
                        PlanNodeActionType.NONE, 100, null, "", List.of()))
        );
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(username, null));
    }
}
