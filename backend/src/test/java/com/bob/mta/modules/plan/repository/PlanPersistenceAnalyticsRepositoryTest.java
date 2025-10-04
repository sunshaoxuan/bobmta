package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanAnalyticsQuery;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;
import com.bob.mta.modules.plan.repository.PlanSearchCriteria;
import com.bob.mta.modules.plan.service.PlanBoardView;
import com.bob.mta.modules.plan.service.PlanBoardViewHelper;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class PlanPersistenceAnalyticsRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bobmta")
            .withUsername("bobmta")
            .withPassword("secret");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private PlanPersistencePlanRepository planRepository;

    @Autowired
    private PlanPersistenceAnalyticsRepository analyticsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private InMemoryPlanRepository inMemoryPlanRepository;
    private InMemoryPlanAnalyticsRepository inMemoryAnalyticsRepository;

    @BeforeAll
    void initializeSchema() {
        PlanPersistenceTestDatabase.initializeSchema(jdbcTemplate);
    }

    @BeforeEach
    void resetDatabase() {
        PlanPersistenceTestDatabase.cleanDatabase(jdbcTemplate);
        inMemoryPlanRepository = new InMemoryPlanRepository();
        inMemoryAnalyticsRepository = new InMemoryPlanAnalyticsRepository(inMemoryPlanRepository);
    }

    @Test
    void shouldMatchAnalyticsWithInMemoryImplementation() {
        OffsetDateTime reference = OffsetDateTime.of(2024, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);

        Plan design = createPlan(planRepository.nextPlanId(), "tenant-analytics", PlanStatus.DESIGN,
                "owner-5", "customer-2", null, null,
                reference.minusDays(30), reference.minusDays(20), List.of());

        Plan scheduledFuture = createPlan(planRepository.nextPlanId(), "tenant-analytics", PlanStatus.SCHEDULED,
                "owner-1", "customer-1", reference.plusDays(1), reference.plusDays(1).plusHours(3),
                reference.minusDays(10), reference.minusDays(5),
                List.of(PlanNodeStatus.DONE, PlanNodeStatus.PENDING));

        Plan overdue = createPlan(planRepository.nextPlanId(), "tenant-analytics", PlanStatus.IN_PROGRESS,
                "owner-1", "customer-3", reference.minusDays(3), reference.minusHours(3),
                reference.minusDays(12), reference.minusDays(1),
                List.of(PlanNodeStatus.DONE, PlanNodeStatus.DONE, PlanNodeStatus.IN_PROGRESS));

        Plan dueSoon = createPlan(planRepository.nextPlanId(), "tenant-analytics", PlanStatus.SCHEDULED,
                "owner-2", "customer-1", reference.plusHours(1), reference.plusHours(3),
                reference.minusDays(8), reference.minusDays(2),
                List.of(PlanNodeStatus.DONE, PlanNodeStatus.PENDING, PlanNodeStatus.PENDING));

        Plan completed = createPlan(planRepository.nextPlanId(), "tenant-analytics", PlanStatus.COMPLETED,
                "owner-3", "customer-4", reference.minusDays(6), reference.minusDays(5),
                reference.minusDays(15), reference.minusDays(6),
                List.of(PlanNodeStatus.DONE, PlanNodeStatus.DONE));

        Plan canceled = createPlan(planRepository.nextPlanId(), "tenant-analytics", PlanStatus.CANCELED,
                "owner-4", "customer-5", reference.minusDays(4), reference.minusDays(2),
                reference.minusDays(18), reference.minusDays(4),
                List.of(PlanNodeStatus.PENDING));

        Plan otherTenant = createPlan(planRepository.nextPlanId(), "tenant-other", PlanStatus.SCHEDULED,
                "owner-x", "customer-x", reference.plusDays(2), reference.plusDays(2).plusHours(2),
                reference.minusDays(9), reference.minusDays(3),
                List.of(PlanNodeStatus.DONE));

        persist(design, scheduledFuture, overdue, dueSoon, completed, canceled, otherTenant);

        PlanAnalyticsQuery query = PlanAnalyticsQuery.builder()
                .tenantId("tenant-analytics")
                .referenceTime(reference)
                .upcomingLimit(5)
                .ownerLimit(5)
                .riskLimit(5)
                .dueSoonMinutes(240)
                .statuses(List.of(PlanStatus.DESIGN, PlanStatus.SCHEDULED, PlanStatus.IN_PROGRESS,
                        PlanStatus.COMPLETED, PlanStatus.CANCELED))
                .build();

        PlanAnalytics persistence = analyticsRepository.summarize(query);
        PlanAnalytics inMemory = inMemoryAnalyticsRepository.summarize(query);

        assertThat(persistence.getTotalPlans()).isEqualTo(inMemory.getTotalPlans());
        assertThat(persistence.getDesignCount()).isEqualTo(inMemory.getDesignCount());
        assertThat(persistence.getScheduledCount()).isEqualTo(inMemory.getScheduledCount());
        assertThat(persistence.getInProgressCount()).isEqualTo(inMemory.getInProgressCount());
        assertThat(persistence.getCompletedCount()).isEqualTo(inMemory.getCompletedCount());
        assertThat(persistence.getCanceledCount()).isEqualTo(inMemory.getCanceledCount());
        assertThat(persistence.getOverdueCount()).isEqualTo(inMemory.getOverdueCount());

        assertThat(upcomingTuples(persistence.getUpcomingPlans()))
                .isEqualTo(upcomingTuples(inMemory.getUpcomingPlans()));
        assertThat(ownerLoadTuples(persistence.getOwnerLoads()))
                .isEqualTo(ownerLoadTuples(inMemory.getOwnerLoads()));
        assertThat(riskPlanTuples(persistence.getRiskPlans()))
                .isEqualTo(riskPlanTuples(inMemory.getRiskPlans()));
    }

    @Test
    void shouldMatchPlanBoardWithInMemoryImplementation() {
        OffsetDateTime baseline = OffsetDateTime.of(2024, 6, 10, 8, 0, 0, 0, ZoneOffset.UTC);

        Plan scheduled = createPlan(planRepository.nextPlanId(), "tenant-board", PlanStatus.SCHEDULED,
                "board-owner-a", "customer-a", baseline.plusHours(1), baseline.plusHours(4),
                baseline.minusDays(2), baseline.minusDays(1), List.of(PlanNodeStatus.DONE, PlanNodeStatus.PENDING));
        Plan inProgress = createPlan(planRepository.nextPlanId(), "tenant-board", PlanStatus.IN_PROGRESS,
                "board-owner-b", "customer-b", baseline.plusDays(1), baseline.plusDays(1).plusHours(3),
                baseline.minusDays(3), baseline.minusDays(2), List.of(PlanNodeStatus.DONE, PlanNodeStatus.DONE));
        Plan completed = createPlan(planRepository.nextPlanId(), "tenant-board", PlanStatus.COMPLETED,
                "board-owner-a", "customer-a", baseline.minusDays(1), baseline.minusDays(1).plusHours(2),
                baseline.minusDays(4), baseline.minusDays(1), List.of(PlanNodeStatus.DONE));
        Plan unknownCustomer = createPlan(planRepository.nextPlanId(), "tenant-board", PlanStatus.SCHEDULED,
                "board-owner-c", null, baseline.plusDays(2), baseline.plusDays(2).plusHours(1),
                baseline.minusDays(5), baseline.minusDays(2), List.of(PlanNodeStatus.PENDING));
        Plan otherTenant = createPlan(planRepository.nextPlanId(), "tenant-other", PlanStatus.SCHEDULED,
                "board-owner-x", "customer-x", baseline.plusDays(3), baseline.plusDays(3).plusHours(2),
                baseline.minusDays(2), baseline.minusDays(1), List.of(PlanNodeStatus.DONE));

        persist(scheduled, inProgress, completed, unknownCustomer, otherTenant);

        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .tenantId("tenant-board")
                .from(baseline.minusDays(3))
                .to(baseline.plusDays(5))
                .build();

        PlanBoardView persistence = analyticsRepository.getPlanBoard(criteria, PlanBoardGrouping.DAY);
        PlanBoardView inMemory = inMemoryAnalyticsRepository.getPlanBoard(criteria, PlanBoardGrouping.DAY);

        assertThat(persistence.getMetrics().getTotalPlans()).isEqualTo(inMemory.getMetrics().getTotalPlans());
        assertThat(persistence.getMetrics().getActivePlans()).isEqualTo(inMemory.getMetrics().getActivePlans());
        assertThat(persistence.getMetrics().getCompletedPlans()).isEqualTo(inMemory.getMetrics().getCompletedPlans());
        assertThat(persistence.getMetrics().getOverduePlans()).isEqualTo(inMemory.getMetrics().getOverduePlans());
        assertThat(persistence.getMetrics().getDueSoonPlans()).isEqualTo(inMemory.getMetrics().getDueSoonPlans());
        assertThat(persistence.getMetrics().getAtRiskPlans()).isEqualTo(inMemory.getMetrics().getAtRiskPlans());
        assertThat(persistence.getMetrics().getAverageProgress())
                .isEqualTo(inMemory.getMetrics().getAverageProgress());
        assertThat(persistence.getMetrics().getAverageDurationHours())
                .isEqualTo(inMemory.getMetrics().getAverageDurationHours());
        assertThat(persistence.getMetrics().getCompletionRate())
                .isEqualTo(inMemory.getMetrics().getCompletionRate());

        assertThat(customerGroupTuples(persistence.getCustomerGroups()))
                .isEqualTo(customerGroupTuples(inMemory.getCustomerGroups()));
        assertThat(bucketTuples(persistence.getTimeBuckets()))
                .isEqualTo(bucketTuples(inMemory.getTimeBuckets()));
        assertThat(planCardTuples(persistence.getCustomerGroups()))
                .isEqualTo(planCardTuples(inMemory.getCustomerGroups()));
        assertThat(persistence.getCustomerGroups())
                .allSatisfy(group -> assertThat(group.getAtRiskPlans())
                        .isEqualTo(group.getOverduePlans() + group.getDueSoonPlans()));
        assertThat(persistence.getTimeBuckets())
                .allSatisfy(bucket -> assertThat(bucket.getAtRiskPlans())
                        .isEqualTo(bucket.getOverduePlans() + bucket.getDueSoonPlans()));
    }

    @Test
    void shouldFilterPlanBoardByTenantAndCustomerList() {
        OffsetDateTime baseline = OffsetDateTime.of(2024, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        Plan includeCustomerA = createPlan(planRepository.nextPlanId(), "tenant-board-filter", PlanStatus.SCHEDULED,
                "owner-a", "cust-keep-a", baseline.plusHours(2), baseline.plusHours(5),
                baseline.minusDays(5), baseline.minusDays(2), List.of(PlanNodeStatus.DONE));
        Plan includeCustomerB = createPlan(planRepository.nextPlanId(), "tenant-board-filter", PlanStatus.IN_PROGRESS,
                "owner-b", "cust-keep-b", baseline.plusDays(1), baseline.plusDays(1).plusHours(3),
                baseline.minusDays(4), baseline.minusDays(1), List.of(PlanNodeStatus.DONE, PlanNodeStatus.PENDING));
        Plan excludedCustomer = createPlan(planRepository.nextPlanId(), "tenant-board-filter", PlanStatus.SCHEDULED,
                "owner-c", "cust-drop", baseline.plusDays(2), baseline.plusDays(2).plusHours(2),
                baseline.minusDays(3), baseline.minusDays(1), List.of(PlanNodeStatus.PENDING));
        Plan otherTenant = createPlan(planRepository.nextPlanId(), "tenant-board-filter-other", PlanStatus.SCHEDULED,
                "owner-d", "cust-keep-a", baseline.plusHours(6), baseline.plusHours(8),
                baseline.minusDays(6), baseline.minusDays(4), List.of(PlanNodeStatus.DONE));

        persist(includeCustomerA, includeCustomerB, excludedCustomer, otherTenant);

        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .tenantId("tenant-board-filter")
                .customerIds(List.of("cust-keep-a", "cust-keep-b"))
                .from(baseline.minusDays(1))
                .to(baseline.plusDays(7))
                .build();

        PlanBoardView persistence = analyticsRepository.getPlanBoard(criteria, PlanBoardGrouping.WEEK);
        PlanBoardView inMemory = inMemoryAnalyticsRepository.getPlanBoard(criteria, PlanBoardGrouping.WEEK);

        assertThat(customerGroupTuples(persistence.getCustomerGroups()))
                .isEqualTo(customerGroupTuples(inMemory.getCustomerGroups()));
        assertThat(bucketTuples(persistence.getTimeBuckets()))
                .isEqualTo(bucketTuples(inMemory.getTimeBuckets()));
        assertThat(persistence.getCustomerGroups())
                .extracting(PlanBoardView.CustomerGroup::getCustomerId)
                .containsExactlyInAnyOrder("cust-keep-a", "cust-keep-b");
    }

    @Test
    void shouldReturnEmptyPlanBoardWhenNoMatchesInPersistence() {
        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .tenantId("tenant-board-empty")
                .from(OffsetDateTime.parse("2024-08-01T00:00:00Z"))
                .to(OffsetDateTime.parse("2024-08-31T00:00:00Z"))
                .build();

        PlanBoardView persistence = analyticsRepository.getPlanBoard(criteria, PlanBoardGrouping.MONTH);
        PlanBoardView inMemory = inMemoryAnalyticsRepository.getPlanBoard(criteria, PlanBoardGrouping.MONTH);

        assertThat(persistence.getMetrics().getTotalPlans()).isZero();
        assertThat(persistence.getCustomerGroups()).isEmpty();
        assertThat(persistence.getTimeBuckets()).isEmpty();
        assertThat(persistence.getMetrics().getCompletionRate()).isZero();
        assertThat(customerGroupTuples(persistence.getCustomerGroups()))
                .isEqualTo(customerGroupTuples(inMemory.getCustomerGroups()));
        assertThat(bucketTuples(persistence.getTimeBuckets()))
                .isEqualTo(bucketTuples(inMemory.getTimeBuckets()));
    }

    @Test
    void shouldSortPlanBoardAggregatesByTotalsAndBucketStart() {
        OffsetDateTime baseline = OffsetDateTime.of(2024, 6, 10, 9, 0, 0, 0, ZoneOffset.UTC);

        Plan custCScheduledA = createPlan(planRepository.nextPlanId(), "tenant-board-order", PlanStatus.SCHEDULED,
                "owner-order", "cust-sort-c", baseline.plusDays(1), baseline.plusDays(1).plusHours(2),
                baseline.minusDays(30), baseline.minusDays(29), List.of(PlanNodeStatus.DONE, PlanNodeStatus.PENDING));
        Plan custCScheduledB = createPlan(planRepository.nextPlanId(), "tenant-board-order", PlanStatus.SCHEDULED,
                "owner-order", "cust-sort-c", baseline.plusDays(1), baseline.plusDays(1).plusHours(3),
                baseline.minusDays(29), baseline.minusDays(28), List.of(PlanNodeStatus.PENDING));
        Plan custCScheduledC = createPlan(planRepository.nextPlanId(), "tenant-board-order", PlanStatus.IN_PROGRESS,
                "owner-order", "cust-sort-c", baseline.plusWeeks(1).plusDays(2), baseline.plusWeeks(1).plusDays(2).plusHours(2),
                baseline.minusDays(28), baseline.minusDays(27), List.of(PlanNodeStatus.DONE));

        Plan custAScheduledA = createPlan(planRepository.nextPlanId(), "tenant-board-order", PlanStatus.SCHEDULED,
                "owner-order", "cust-sort-a", baseline.minusWeeks(1).plusDays(2), baseline.minusWeeks(1).plusDays(2).plusHours(2),
                baseline.minusDays(27), baseline.minusDays(26), List.of(PlanNodeStatus.DONE));
        Plan custAScheduledB = createPlan(planRepository.nextPlanId(), "tenant-board-order", PlanStatus.COMPLETED,
                "owner-order", "cust-sort-a", baseline.plusWeeks(2).plusDays(1), baseline.plusWeeks(2).plusDays(1).plusHours(1),
                baseline.minusDays(26), baseline.minusDays(25), List.of(PlanNodeStatus.DONE));

        Plan custBScheduledA = createPlan(planRepository.nextPlanId(), "tenant-board-order", PlanStatus.SCHEDULED,
                "owner-order", "cust-sort-b", baseline.minusWeeks(2).plusDays(3), baseline.minusWeeks(2).plusDays(3).plusHours(3),
                baseline.minusDays(25), baseline.minusDays(24), List.of(PlanNodeStatus.PENDING));
        Plan custBScheduledB = createPlan(planRepository.nextPlanId(), "tenant-board-order", PlanStatus.IN_PROGRESS,
                "owner-order", "cust-sort-b", baseline.plusWeeks(3).plusDays(4), baseline.plusWeeks(3).plusDays(4).plusHours(2),
                baseline.minusDays(24), baseline.minusDays(23), List.of(PlanNodeStatus.DONE));

        persist(custCScheduledA, custCScheduledB, custCScheduledC,
                custAScheduledA, custAScheduledB,
                custBScheduledA, custBScheduledB);

        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .tenantId("tenant-board-order")
                .from(baseline.minusWeeks(3))
                .to(baseline.plusWeeks(4))
                .build();

        PlanBoardView board = analyticsRepository.getPlanBoard(criteria, PlanBoardGrouping.WEEK);

        assertThat(board.getCustomerGroups())
                .extracting(PlanBoardView.CustomerGroup::getCustomerId)
                .containsExactly("cust-sort-c", "cust-sort-a", "cust-sort-b");
        assertThat(board.getCustomerGroups())
                .extracting(PlanBoardView.CustomerGroup::getTotalPlans)
                .containsExactly(3L, 2L, 2L);

        assertThat(board.getTimeBuckets())
                .extracting(PlanBoardView.TimeBucket::getStart)
                .isSorted();

        OffsetDateTime expectedBucketStart = PlanBoardViewHelper.normalizeBucketStart(
                baseline.plusDays(1), PlanBoardGrouping.WEEK);

        PlanBoardView.TimeBucket weekBucket = board.getTimeBuckets().stream()
                .filter(bucket -> expectedBucketStart.equals(bucket.getStart()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected bucket for week starting " + expectedBucketStart));

        assertThat(weekBucket.getPlans())
                .isSortedAccordingTo(PlanBoardViewHelper.PLAN_CARD_COMPARATOR);
    }

    private void persist(Plan... plans) {
        for (Plan plan : plans) {
            planRepository.save(plan);
            inMemoryPlanRepository.save(plan);
        }
    }

    private Plan createPlan(String planId,
                            String tenantId,
                            PlanStatus status,
                            String owner,
                            String customerId,
                            OffsetDateTime plannedStart,
                            OffsetDateTime plannedEnd,
                            OffsetDateTime createdAt,
                            OffsetDateTime updatedAt,
                            List<PlanNodeStatus> executionStatuses) {
        List<PlanNode> nodes = new ArrayList<>();
        List<PlanNodeExecution> executions = new ArrayList<>();

        for (int index = 0; index < executionStatuses.size(); index++) {
            String nodeId = planRepository.nextNodeId();
            PlanNode node = new PlanNode(nodeId, "Node-" + index, "TASK", owner, index,
                    60, PlanNodeActionType.MANUAL, 100, null, "Node description " + index, List.of());
            nodes.add(node);

            OffsetDateTime start = plannedStart != null ? plannedStart.minusHours(1).plusHours(index)
                    : createdAt.plusHours(index);
            OffsetDateTime end = executionStatuses.get(index) == PlanNodeStatus.DONE
                    ? start.plusHours(1)
                    : null;
            PlanNodeExecution execution = new PlanNodeExecution(nodeId, executionStatuses.get(index),
                    start, end, owner, executionStatuses.get(index).name().toLowerCase(), null, List.of());
            executions.add(execution);
        }

        return new Plan(planId, tenantId, "Plan " + planId, "Description for " + planId,
                customerId, owner, List.of("participant-" + planId), status,
                plannedStart, plannedEnd, null, null, null, null, null,
                "UTC", nodes, executions, createdAt, updatedAt, List.of(), PlanReminderPolicy.empty());
    }

    private List<Tuple> upcomingTuples(List<PlanAnalytics.UpcomingPlan> plans) {
        return plans.stream()
                .map(plan -> tuple(plan.getId(), plan.getTitle(), plan.getStatus(),
                        plan.getPlannedStartTime(), plan.getPlannedEndTime(),
                        plan.getOwner(), plan.getCustomerId(), plan.getProgress()))
                .toList();
    }

    private List<Tuple> ownerLoadTuples(List<PlanAnalytics.OwnerLoad> loads) {
        return loads.stream()
                .map(load -> tuple(load.getOwnerId(), load.getTotalPlans(),
                        load.getActivePlans(), load.getOverduePlans()))
                .toList();
    }

    private List<Tuple> riskPlanTuples(List<PlanAnalytics.RiskPlan> plans) {
        return plans.stream()
                .map(plan -> tuple(plan.getId(), plan.getRiskLevel(),
                        plan.getMinutesUntilDue(), plan.getMinutesOverdue()))
                .toList();
    }

    private List<Tuple> customerGroupTuples(List<PlanBoardView.CustomerGroup> groups) {
        return groups.stream()
                .map(group -> tuple(group.getCustomerId(), group.getTotalPlans(), group.getActivePlans(),
                        group.getCompletedPlans(), group.getOverduePlans(), group.getDueSoonPlans(),
                        group.getAtRiskPlans(), group.getAverageProgress(), group.getEarliestStart(),
                        group.getLatestEnd()))
                .toList();
    }

    private List<Tuple> bucketTuples(List<PlanBoardView.TimeBucket> buckets) {
        return buckets.stream()
                .map(bucket -> tuple(bucket.getBucketId(), bucket.getTotalPlans(), bucket.getActivePlans(),
                        bucket.getCompletedPlans(), bucket.getOverduePlans(), bucket.getDueSoonPlans(),
                        bucket.getAtRiskPlans()))
                .toList();
    }

    private List<Tuple> planCardTuples(List<PlanBoardView.CustomerGroup> groups) {
        return groups.stream()
                .flatMap(group -> group.getPlans().stream()
                        .map(card -> tuple(group.getCustomerId(), card.getId(), card.getStatus(),
                                card.getPlannedStartTime(), card.getPlannedEndTime(), card.getProgress(),
                                card.isOverdue(), card.isDueSoon(), card.getMinutesUntilDue(), card.getMinutesOverdue())))
                .toList();
    }
}

