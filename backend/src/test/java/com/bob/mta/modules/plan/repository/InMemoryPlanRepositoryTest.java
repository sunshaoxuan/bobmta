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

    @Test
    void shouldFilterByCriteria() {
        OffsetDateTime baseline = OffsetDateTime.now();
        Plan matching = new Plan(
                "PLAN-101", "tenant-z", "上海巡检", "巡检前准备", "cust-88", "owner-a",
                List.of("owner-a"), PlanStatus.SCHEDULED,
                baseline.plusDays(1), baseline.plusDays(1).plusHours(1),
                null, null, null, null, null,
                "Asia/Shanghai", List.of(), List.of(), OffsetDateTime.now(), OffsetDateTime.now(),
                List.of(), PlanReminderPolicy.empty()
        );
        Plan nonMatching = new Plan(
                "PLAN-102", "tenant-z", "东京升级", "系统升级", "cust-99", "owner-b",
                List.of("owner-b"), PlanStatus.DESIGN,
                baseline.plusDays(3), baseline.plusDays(3).plusHours(2),
                null, null, null, null, null,
                "Asia/Tokyo", List.of(), List.of(), OffsetDateTime.now(), OffsetDateTime.now(),
                List.of(), PlanReminderPolicy.empty()
        );

        repository.save(matching);
        repository.save(nonMatching);

        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .tenantId("tenant-z")
                .customerId("cust-88")
                .owner("owner-a")
                .keyword("巡检")
                .status(PlanStatus.SCHEDULED)
                .from(baseline)
                .to(baseline.plusDays(2))
                .build();

        assertThat(repository.countByCriteria(criteria)).isEqualTo(1);
        assertThat(repository.findByCriteria(criteria))
                .containsExactly(matching);
    }

    @Test
    void shouldApplyPagination() {
        OffsetDateTime baseline = OffsetDateTime.now();
        for (int i = 0; i < 5; i++) {
            Plan plan = new Plan(
                    "PLAN-20" + i, "tenant-page", "计划" + i, "描述", "cust-1", "owner",
                    List.of("owner"), PlanStatus.DESIGN,
                    baseline.plusDays(i), baseline.plusDays(i).plusHours(1),
                    null, null, null, null, null,
                    "Asia/Shanghai", List.of(), List.of(), OffsetDateTime.now(), OffsetDateTime.now(),
                    List.of(), PlanReminderPolicy.empty()
            );
            repository.save(plan);
        }

        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .tenantId("tenant-page")
                .limit(2)
                .offset(2)
                .build();

        List<Plan> result = repository.findByCriteria(criteria);

        assertThat(repository.countByCriteria(criteria)).isEqualTo(5);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("PLAN-202");
    }
}
