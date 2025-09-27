package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanReminderPolicy;
import com.bob.mta.modules.plan.domain.PlanStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPlanRepositoryTest {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();

    @Test
    void shouldGenerateSequentialIdentifiers() {
        String firstPlanId = repository.nextPlanId();
        String secondPlanId = repository.nextPlanId();
        String firstNodeId = repository.nextNodeId();
        String secondNodeId = repository.nextNodeId();
        String reminderId = repository.nextReminderId();

        assertThat(firstPlanId).isNotEqualTo(secondPlanId);
        assertThat(firstNodeId).isNotEqualTo(secondNodeId);
        assertThat(reminderId).startsWith("REM-");
    }

    @Test
    void shouldStoreAndRetrievePlan() {
        Plan plan = new Plan(
                "PLAN-100", "tenant-x", "测试计划", "desc", "cust-1", "admin",
                List.of("admin"), PlanStatus.DESIGN,
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(2),
                null, null, null, null, null,
                "Asia/Shanghai", List.of(), List.of(), OffsetDateTime.now(), OffsetDateTime.now(),
                List.of(), PlanReminderPolicy.empty()
        );

        repository.save(plan);

        assertThat(repository.findById("PLAN-100")).contains(plan);
        assertThat(repository.findAll()).contains(plan);

        repository.delete("PLAN-100");

        assertThat(repository.findById("PLAN-100")).isEmpty();
    }
}
