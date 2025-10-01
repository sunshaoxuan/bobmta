package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.persistence.PlanPersistencePlanRepository;
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

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class PlanPersistencePlanRepositoryTest {

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
    private PlanPersistencePlanRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void initializeSchema() {
        assertThat(dataSource).isNotNull();
        PlanPersistenceTestDatabase.initializeSchema(jdbcTemplate);
    }

    @BeforeEach
    void cleanDatabase() {
        PlanPersistenceTestDatabase.cleanDatabase(jdbcTemplate);
    }

    @Test
    void shouldPersistAndLoadFullAggregate() {
        OffsetDateTime now = OffsetDateTime.of(2024, 5, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        String planId = repository.nextPlanId();
        String rootNodeId = repository.nextNodeId();
        String childNodeId = repository.nextNodeId();
        String reminderId = repository.nextReminderId();

        PlanNode child = new PlanNode(childNodeId, "Child", "TASK", "operator-b", 1,
                30, PlanNodeActionType.MANUAL, 80, "child-action", "Child description", List.of());
        PlanNode root = new PlanNode(rootNodeId, "Root", "TASK", "operator-a", 0,
                90, PlanNodeActionType.API_CALL, 60, "root-action", "Root description", List.of(child));

        PlanNodeExecution rootExecution = new PlanNodeExecution(rootNodeId, PlanNodeStatus.DONE,
                now.minusHours(2), now.minusHours(1), "user-1", "completed", "log-entry",
                List.of("file-1", "file-2"));
        PlanNodeExecution childExecution = new PlanNodeExecution(childNodeId, PlanNodeStatus.IN_PROGRESS,
                now.minusHours(1), null, "user-2", "running", null,
                List.of("file-3"));

        PlanActivity activity = new PlanActivity(PlanActivityType.PLAN_CREATED, now.minusMinutes(30),
                "system", "plan.created", "ref-1", Map.of("source", "test"));

        PlanReminderRule rule = new PlanReminderRule(reminderId, PlanReminderTrigger.BEFORE_START, 45,
                List.of("EMAIL", "SMS"), "template-1", List.of("user-1", "user-2"),
                "Primary reminder", true);
        PlanReminderPolicy policy = new PlanReminderPolicy(List.of(rule), now.minusMinutes(45), "scheduler");

        Plan plan = new Plan(planId, "tenant-1", "Persistence Plan", "Detailed plan",
                "customer-7", "owner-1", List.of("participant-1", "participant-2"), PlanStatus.SCHEDULED,
                now.plusDays(1), now.plusDays(2), now.minusDays(2), now.minusDays(1),
                null, null, null, "UTC", List.of(root), List.of(rootExecution, childExecution),
                now.minusDays(3), now.minusHours(1), List.of(activity), policy);

        repository.save(plan);

        Plan reloaded = repository.findById(planId).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(planId);
        assertThat(reloaded.getParticipants()).containsExactly("participant-1", "participant-2");
        assertThat(reloaded.getNodes()).hasSize(1);
        PlanNode persistedRoot = reloaded.getNodes().get(0);
        assertThat(persistedRoot.getId()).isEqualTo(rootNodeId);
        assertThat(persistedRoot.getChildren()).hasSize(1);
        assertThat(persistedRoot.getChildren().get(0).getId()).isEqualTo(childNodeId);

        Map<String, PlanNodeExecution> executionsByNode = reloaded.getExecutions().stream()
                .collect(Collectors.toMap(PlanNodeExecution::getNodeId, exec -> exec));
        assertThat(executionsByNode.get(rootNodeId).getStatus()).isEqualTo(PlanNodeStatus.DONE);
        assertThat(executionsByNode.get(rootNodeId).getFileIds()).containsExactly("file-1", "file-2");
        assertThat(executionsByNode.get(childNodeId).getFileIds()).containsExactly("file-3");

        assertThat(reloaded.getActivities()).hasSize(1);
        assertThat(reloaded.getActivities().get(0).getAttributes()).containsEntry("source", "test");

        PlanReminderPolicy persistedPolicy = reloaded.getReminderPolicy();
        assertThat(persistedPolicy.getUpdatedAt()).isEqualTo(policy.getUpdatedAt());
        assertThat(persistedPolicy.getUpdatedBy()).isEqualTo("scheduler");
        assertThat(persistedPolicy.getRules()).hasSize(1);
        PlanReminderRule persistedRule = persistedPolicy.getRules().get(0);
        assertThat(persistedRule.getId()).isEqualTo(reminderId);
        assertThat(persistedRule.getTrigger()).isEqualTo(PlanReminderTrigger.BEFORE_START);
        assertThat(persistedRule.getChannels()).containsExactly("EMAIL", "SMS");
        assertThat(persistedRule.getRecipients()).containsExactly("user-1", "user-2");
    }

    @Test
    void shouldReplaceAssociationsOnUpdate() {
        OffsetDateTime now = OffsetDateTime.of(2024, 6, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        String planId = repository.nextPlanId();
        Plan initialPlan = new Plan(planId, "tenant-1", "Initial", "",
                "customer-1", "owner-1", List.of("p1"), PlanStatus.SCHEDULED,
                now.plusDays(1), now.plusDays(2), null, null, null, null, null,
                "UTC", List.of(), List.of(), now.minusDays(1), now.minusHours(1), List.of(),
                PlanReminderPolicy.empty());

        repository.save(initialPlan);

        String newNodeId = repository.nextNodeId();
        PlanNode newNode = new PlanNode(newNodeId, "Updated Node", "TASK", "owner-2", 0,
                null, PlanNodeActionType.NONE, 100, null, "Updated", List.of());
        PlanNodeExecution execution = new PlanNodeExecution(newNodeId, PlanNodeStatus.DONE,
                now, now.plusHours(1), "owner-2", "done", null, List.of());
        PlanReminderRule rule = new PlanReminderRule(repository.nextReminderId(), PlanReminderTrigger.BEFORE_END, 15,
                List.of("EMAIL"), null, List.of(), "Updated rule", false);
        PlanReminderPolicy reminderPolicy = new PlanReminderPolicy(List.of(rule), now, "owner-2");
        Plan updatedPlan = new Plan(planId, "tenant-1", "Initial", "Updated description",
                "customer-1", "owner-2", List.of("p2", "p3"), PlanStatus.IN_PROGRESS,
                now.plusDays(3), now.plusDays(4), null, null, null, null, null,
                "UTC", List.of(newNode), List.of(execution), now.minusDays(1), now,
                List.of(new PlanActivity(PlanActivityType.PLAN_UPDATED, now, "owner-2",
                        "plan.updated", null, Map.of())), reminderPolicy);

        repository.save(updatedPlan);

        Plan reloaded = repository.findById(planId).orElseThrow();
        assertThat(reloaded.getOwner()).isEqualTo("owner-2");
        assertThat(reloaded.getParticipants()).containsExactly("p2", "p3");
        assertThat(reloaded.getNodes()).extracting(PlanNode::getId).containsExactly(newNodeId);
        assertThat(reloaded.getExecutions()).hasSize(1);
        assertThat(reloaded.getActivities()).hasSize(1);
        assertThat(reloaded.getReminderPolicy().getRules()).hasSize(1);
        assertThat(countRows("mt_plan_node_attachment")).isZero();
    }

    @Test
    void shouldDeletePlanAndAssociations() {
        OffsetDateTime now = OffsetDateTime.of(2024, 4, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        String planId = repository.nextPlanId();
        Plan plan = new Plan(planId, "tenant-1", "To delete", "",
                "customer-9", "owner-9", List.of("px"), PlanStatus.CANCELED,
                now, now.plusHours(2), null, null, "reason", "owner-9", now,
                "UTC", List.of(), List.of(), now.minusDays(2), now.minusHours(1), List.of(),
                PlanReminderPolicy.empty());

        repository.save(plan);
        assertThat(repository.findById(planId)).isPresent();

        repository.delete(planId);
        assertThat(repository.findById(planId)).isEmpty();
        assertThat(PlanPersistenceTestDatabase.tableNames().stream()
                .map(table -> PlanPersistenceTestDatabase.countRows(jdbcTemplate, table))
                .collect(Collectors.toList()))
                .allMatch(count -> count == 0L);
    }

    @Test
    void shouldMatchInMemoryPaginationAndCounts() {
        OffsetDateTime base = OffsetDateTime.of(2024, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        InMemoryPlanRepository memoryRepository = new InMemoryPlanRepository();
        for (int index = 0; index < 5; index++) {
            String planId = repository.nextPlanId();
            OffsetDateTime start = base.plusDays(index);
            Plan plan = new Plan(planId, "tenant-1", "Project-" + index, "Description " + index,
                    "customer-" + (index % 2), "owner-" + (index % 3),
                    List.of("participant-" + index), index % 2 == 0 ? PlanStatus.SCHEDULED : PlanStatus.IN_PROGRESS,
                    start, start.plusHours(4), null, null, null, null, null, "UTC",
                    List.of(), List.of(), start.minusDays(1), start.minusHours(1), List.of(),
                    PlanReminderPolicy.empty());
            repository.save(plan);
            memoryRepository.save(plan);
        }

        List<PlanSearchCriteria> criteriaList = List.of(
                PlanSearchCriteria.builder().tenantId("tenant-1").limit(2).offset(0).build(),
                PlanSearchCriteria.builder().tenantId("tenant-1").limit(2).offset(2).build(),
                PlanSearchCriteria.builder().tenantId("tenant-1").owner("owner-1").build(),
                PlanSearchCriteria.builder().tenantId("tenant-1").keyword("Project-3").build(),
                PlanSearchCriteria.builder().tenantId("tenant-1").status(PlanStatus.SCHEDULED).build()
        );

        for (PlanSearchCriteria criteria : criteriaList) {
            List<String> persistenceIds = repository.findByCriteria(criteria).stream()
                    .map(Plan::getId)
                    .toList();
            List<String> memoryIds = memoryRepository.findByCriteria(criteria).stream()
                    .map(Plan::getId)
                    .toList();
            assertThat(persistenceIds).isEqualTo(memoryIds);
            assertThat(repository.countByCriteria(criteria))
                    .isEqualTo(memoryRepository.countByCriteria(criteria));
        }
    }

    @Test
    void shouldGenerateSequentialIdentifiers() {
        String id1 = repository.nextPlanId();
        String id2 = repository.nextPlanId();
        assertThat(id1).startsWith("PLAN-");
        assertThat(id2).isNotEqualTo(id1);

        String node1 = repository.nextNodeId();
        String node2 = repository.nextNodeId();
        assertThat(node1).startsWith("NODE-");
        assertThat(Set.of(node1, node2)).hasSize(2);

        String reminder1 = repository.nextReminderId();
        String reminder2 = repository.nextReminderId();
        assertThat(reminder1).startsWith("REM-");
        assertThat(reminder2).isNotEqualTo(reminder1);
    }

}
