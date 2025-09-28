package com.bob.mta.modules.plan.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

class PlanPersistenceMapperTest {

    @Test
    void shouldConvertPlanRoundTrip() {
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

        Plan plan = new Plan(
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

        PlanAggregate aggregate = PlanPersistenceMapper.toAggregate(plan);
        Plan converted = PlanPersistenceMapper.toDomain(aggregate);

        assertThat(aggregate.plan().id()).isEqualTo("plan-1");
        assertThat(aggregate.nodes()).hasSize(2);
        assertThat(aggregate.executions()).hasSize(2);
        assertThat(aggregate.activities()).hasSize(1);
        assertThat(aggregate.reminderRules()).hasSize(1);

        assertThat(converted.getId()).isEqualTo(plan.getId());
        assertThat(converted.getTenantId()).isEqualTo(plan.getTenantId());
        assertThat(converted.getTitle()).isEqualTo(plan.getTitle());
        assertThat(converted.getParticipants()).containsExactlyElementsOf(plan.getParticipants());
        assertThat(converted.getNodes()).hasSize(1);
        assertThat(converted.getNodes().get(0).getChildren()).hasSize(1);
        assertThat(converted.getExecutions()).hasSize(2);
        assertThat(converted.getExecutions().get(0).getFileIds()).containsExactly("file-1");
        assertThat(converted.getReminderPolicy().getRules()).hasSize(1);
        assertThat(converted.getReminderPolicy().getUpdatedBy()).isEqualTo("operator-2");
    }
}
