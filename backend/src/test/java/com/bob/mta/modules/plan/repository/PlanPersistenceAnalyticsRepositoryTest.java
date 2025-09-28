package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanAnalytics;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.persistence.PlanAggregateMapper;
import com.bob.mta.modules.plan.persistence.PlanAnalyticsQueryParameters;
import com.bob.mta.modules.plan.persistence.PlanStatusCountEntity;
import com.bob.mta.modules.plan.persistence.PlanUpcomingPlanEntity;
import com.bob.mta.modules.plan.repository.PlanAnalyticsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanPersistenceAnalyticsRepositoryTest {

    @Mock
    private PlanAggregateMapper mapper;

    private PlanPersistenceAnalyticsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PlanPersistenceAnalyticsRepository(mapper);
    }

    @Test
    void shouldSummarizeCountsAndUpcomingPlans() {
        OffsetDateTime now = OffsetDateTime.now();
        PlanAnalyticsQuery query = PlanAnalyticsQuery.builder()
                .tenantId("tenant-1")
                .from(now.minusDays(1))
                .to(now.plusDays(7))
                .referenceTime(now)
                .upcomingLimit(3)
                .build();

        when(mapper.countPlansByStatus(any(PlanAnalyticsQueryParameters.class))).thenReturn(List.of(
                new PlanStatusCountEntity(PlanStatus.DESIGN, 2),
                new PlanStatusCountEntity(PlanStatus.SCHEDULED, 1),
                new PlanStatusCountEntity(PlanStatus.IN_PROGRESS, 1)
        ));
        when(mapper.countOverduePlans(any(PlanAnalyticsQueryParameters.class))).thenReturn(1L);
        when(mapper.findUpcomingPlans(any(PlanAnalyticsQueryParameters.class))).thenReturn(List.of(
                new PlanUpcomingPlanEntity("plan-1", "タイトル", PlanStatus.SCHEDULED,
                        now.plusDays(1), now.plusDays(1).plusHours(2), "owner-1", "cust-1", 1L, 4L)
        ));

        PlanAnalytics analytics = repository.summarize(query);

        assertThat(analytics.getTotalPlans()).isEqualTo(4);
        assertThat(analytics.getDesignCount()).isEqualTo(2);
        assertThat(analytics.getScheduledCount()).isEqualTo(1);
        assertThat(analytics.getInProgressCount()).isEqualTo(1);
        assertThat(analytics.getOverdueCount()).isEqualTo(1);
        assertThat(analytics.getUpcomingPlans()).hasSize(1);
        assertThat(analytics.getUpcomingPlans().get(0).getProgress()).isEqualTo(25);

        ArgumentCaptor<PlanAnalyticsQueryParameters> captor = ArgumentCaptor.forClass(PlanAnalyticsQueryParameters.class);
        verify(mapper).countPlansByStatus(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo("tenant-1");
    }

}
