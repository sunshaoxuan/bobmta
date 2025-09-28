package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanActivity;
import com.bob.mta.modules.plan.domain.PlanActivityType;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanNodeExecution;
import com.bob.mta.modules.plan.domain.PlanNodeStatus;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.domain.PlanReminderRule;
import com.bob.mta.modules.plan.domain.PlanReminderTrigger;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.persistence.PlanAggregate;
import com.bob.mta.modules.plan.persistence.PlanAggregateMapper;
import com.bob.mta.modules.plan.persistence.PlanEntity;
import com.bob.mta.modules.plan.persistence.PlanPersistenceMapper;
import com.bob.mta.modules.plan.repository.PlanSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanPersistencePlanRepositoryTest {

    @Mock
    private PlanAggregateMapper mapper;

    @InjectMocks
    private PlanPersistencePlanRepository repository;

    @Captor
    private ArgumentCaptor<List<?>> listCaptor;

    private Plan samplePlan;
    private PlanAggregate aggregate;

    @BeforeEach
    void setUp() {
        samplePlan = buildSamplePlan();
        aggregate = PlanPersistenceMapper.toAggregate(samplePlan);
    }

    @Test
    void shouldLoadPlanById() {
        when(mapper.findPlanById("plan-1")).thenReturn(aggregate.plan());
        when(mapper.findParticipantsByPlanIds(List.of("plan-1"))).thenReturn(aggregate.participants());
        when(mapper.findNodesByPlanIds(List.of("plan-1"))).thenReturn(aggregate.nodes());
        when(mapper.findExecutionsByPlanIds(List.of("plan-1"))).thenReturn(aggregate.executions());
        when(mapper.findAttachmentsByPlanIds(List.of("plan-1"))).thenReturn(aggregate.attachments());
        when(mapper.findActivitiesByPlanIds(List.of("plan-1"))).thenReturn(aggregate.activities());
        when(mapper.findReminderRulesByPlanIds(List.of("plan-1"))).thenReturn(aggregate.reminderRules());

        Optional<Plan> result = repository.findById("plan-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("plan-1");
        assertThat(result.get().getNodes()).hasSize(1);
        verify(mapper).findPlanById("plan-1");
        verify(mapper).findParticipantsByPlanIds(List.of("plan-1"));
        verify(mapper).findNodesByPlanIds(List.of("plan-1"));
        verify(mapper).findExecutionsByPlanIds(List.of("plan-1"));
        verify(mapper).findAttachmentsByPlanIds(List.of("plan-1"));
        verify(mapper).findActivitiesByPlanIds(List.of("plan-1"));
        verify(mapper).findReminderRulesByPlanIds(List.of("plan-1"));
    }

    @Test
    void shouldReturnEmptyWhenPlanNotFound() {
        when(mapper.findPlanById("missing")).thenReturn(null);

        Optional<Plan> result = repository.findById("missing");

        assertThat(result).isEmpty();
        verify(mapper).findPlanById("missing");
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void shouldSaveNewPlan() {
        when(mapper.findPlanById("plan-1")).thenReturn(null);

        repository.save(samplePlan);

        verify(mapper).insertPlan(aggregate.plan());
        verify(mapper, never()).updatePlan(any());
        verify(mapper).insertParticipants(listCaptor.capture());
        assertThat(listCaptor.getValue()).hasSize(2);
        verify(mapper).insertNodes(any());
        verify(mapper).insertExecutions(any());
        verify(mapper).insertAttachments(any());
        verify(mapper).insertActivities(any());
        verify(mapper).insertReminderRules(any());
    }

    @Test
    void shouldUpdateExistingPlan() {
        PlanEntity existing = aggregate.plan();
        when(mapper.findPlanById("plan-1")).thenReturn(existing);

        repository.save(samplePlan);

        verify(mapper).updatePlan(existing);
        verify(mapper).deleteAttachments("plan-1");
        verify(mapper).deleteExecutions("plan-1");
        verify(mapper).deleteNodes("plan-1");
        verify(mapper).deleteParticipants("plan-1");
        verify(mapper).deleteActivities("plan-1");
        verify(mapper).deleteReminderRules("plan-1");
        verify(mapper).insertParticipants(any());
        verify(mapper).insertNodes(any());
        verify(mapper).insertExecutions(any());
        verify(mapper).insertAttachments(any());
        verify(mapper).insertActivities(any());
        verify(mapper).insertReminderRules(any());
    }

    @Test
    void shouldDeletePlan() {
        repository.delete("plan-1");

        verify(mapper).deleteAttachments("plan-1");
        verify(mapper).deleteExecutions("plan-1");
        verify(mapper).deleteNodes("plan-1");
        verify(mapper).deleteParticipants("plan-1");
        verify(mapper).deleteActivities("plan-1");
        verify(mapper).deleteReminderRules("plan-1");
        verify(mapper).deletePlan("plan-1");
    }

    @Test
    void shouldFindPlansByCriteria() {
        when(mapper.findPlans(any())).thenReturn(List.of(aggregate.plan()));
        when(mapper.findParticipantsByPlanIds(List.of("plan-1"))).thenReturn(aggregate.participants());
        when(mapper.findNodesByPlanIds(List.of("plan-1"))).thenReturn(aggregate.nodes());
        when(mapper.findExecutionsByPlanIds(List.of("plan-1"))).thenReturn(aggregate.executions());
        when(mapper.findAttachmentsByPlanIds(List.of("plan-1"))).thenReturn(aggregate.attachments());
        when(mapper.findActivitiesByPlanIds(List.of("plan-1"))).thenReturn(aggregate.activities());
        when(mapper.findReminderRulesByPlanIds(List.of("plan-1"))).thenReturn(aggregate.reminderRules());

        List<Plan> plans = repository.findByCriteria(PlanSearchCriteria.builder()
                .tenantId("tenant-1")
                .status(PlanStatus.IN_PROGRESS)
                .build());

        assertThat(plans).hasSize(1);
        verify(mapper).findPlans(any());
        verify(mapper).findParticipantsByPlanIds(List.of("plan-1"));
        verify(mapper).findNodesByPlanIds(List.of("plan-1"));
        verify(mapper).findExecutionsByPlanIds(List.of("plan-1"));
        verify(mapper).findAttachmentsByPlanIds(List.of("plan-1"));
        verify(mapper).findActivitiesByPlanIds(List.of("plan-1"));
        verify(mapper).findReminderRulesByPlanIds(List.of("plan-1"));
    }

    @Test
    void shouldProvideSequenceDelegation() {
        when(mapper.nextPlanId()).thenReturn("PLAN-100");
        when(mapper.nextNodeId()).thenReturn("NODE-200");
        when(mapper.nextReminderId()).thenReturn("REM-300");

        assertThat(repository.nextPlanId()).isEqualTo("PLAN-100");
        assertThat(repository.nextNodeId()).isEqualTo("NODE-200");
        assertThat(repository.nextReminderId()).isEqualTo("REM-300");
    }

    private Plan buildSamplePlan() {
        OffsetDateTime now = OffsetDateTime.now();
        PlanNode childNode = new PlanNode(
                "node-2",
                "node-2-name",
                "CHECK",
                "assignee-2",
                2,
                30,
                "action-2",
                "description-2",
                List.of()
        );
        PlanNode rootNode = new PlanNode(
                "node-1",
                "node-1-name",
                "CHECK",
                "assignee-1",
                1,
                60,
                "action-1",
                "description-1",
                List.of(childNode)
        );
        PlanNodeExecution executionOne = new PlanNodeExecution(
                "node-1",
                PlanNodeStatus.DONE,
                now.minusHours(2),
                now.minusHours(1),
                "operator-1",
                "result-1",
                "log-1",
                List.of("file-1")
        );
        PlanNodeExecution executionTwo = new PlanNodeExecution(
                "node-2",
                PlanNodeStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
        PlanActivity activity = new PlanActivity(
                PlanActivityType.PLAN_CREATED,
                now.minusDays(1),
                "actor-1",
                "message-1",
                "reference-1",
                Map.of("scope", "primary")
        );
        PlanReminderRule rule = new PlanReminderRule(
                "rule-1",
                PlanReminderTrigger.BEFORE_PLAN_START,
                45,
                List.of("EMAIL"),
                "template-1",
                List.of("OWNER"),
                "description-1"
        );
        PlanReminderPolicy policy = new PlanReminderPolicy(List.of(rule), now.minusMinutes(10), "operator-2");
        return new Plan(
                "plan-1",
                "tenant-1",
                "title-1",
                "description-1",
                "customer-1",
                "owner-1",
                List.of("owner-1", "participant-1"),
                PlanStatus.IN_PROGRESS,
                now.plusDays(1),
                now.plusDays(1).plusHours(4),
                now.minusHours(3),
                now.minusHours(1),
                null,
                null,
                null,
                "Asia/Tokyo",
                List.of(rootNode),
                List.of(executionOne, executionTwo),
                now.minusDays(2),
                now,
                List.of(activity),
                policy
        );
    }
}
