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
}

