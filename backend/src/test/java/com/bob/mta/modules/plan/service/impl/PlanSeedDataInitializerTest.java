package com.bob.mta.modules.plan.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.repository.PlanRepository;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanSeedDataInitializerTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanService planService;

    private PlanSeedDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new PlanSeedDataInitializer(planRepository, planService);
    }

    @Test
    void shouldSeedDefaultPlansWhenRepositoryEmpty() throws Exception {
        when(planRepository.findAll()).thenReturn(List.of());

        initializer.run(null);

        verify(planRepository).findAll();
        verify(planService, times(2)).createPlan(any(CreatePlanCommand.class));
    }

    @Test
    void shouldSkipSeedingWhenDataExists() throws Exception {
        when(planRepository.findAll()).thenReturn(List.of(mock(Plan.class)));

        initializer.run(null);

        verify(planRepository).findAll();
        verify(planService, never()).createPlan(any(CreatePlanCommand.class));
    }
}
