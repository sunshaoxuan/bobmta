package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;
import com.bob.mta.modules.plan.service.PlanBoardView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanBoardResponseTest {

    @Test
    @DisplayName("from() should return empty response when source view is null")
    void shouldReturnEmptyResponseWhenViewIsNull() {
        PlanBoardResponse response = PlanBoardResponse.from(null);

        assertThat(response.getCustomerGroups()).isEmpty();
        assertThat(response.getTimeBuckets()).isEmpty();
        assertThat(response.getMetrics().getTotalPlans()).isZero();
        assertThat(response.getGranularity()).isNull();
        assertThat(response.getReferenceTime()).isNull();
    }

    @Test
    @DisplayName("from() should provide zero metrics when source view omits metrics")
    void shouldProvideDefaultMetricsWhenSourceHasNoMetrics() {
        OffsetDateTime reference = OffsetDateTime.parse("2024-06-01T00:00:00Z");
        PlanBoardView view = new PlanBoardView(List.of(), List.of(), null, PlanBoardGrouping.DAY, reference);

        PlanBoardResponse response = PlanBoardResponse.from(view);

        assertThat(response.getMetrics()).isNotNull();
        assertThat(response.getMetrics().getTotalPlans()).isZero();
        assertThat(response.getMetrics().getCompletionRate()).isZero();
        assertThat(response.getMetrics().getAverageDurationHours()).isZero();
        assertThat(response.getMetrics().getAtRiskPlans()).isZero();
        assertThat(response.getGranularity()).isEqualTo(PlanBoardGrouping.DAY.name());
        assertThat(response.getReferenceTime()).isEqualTo(reference);
        assertThat(response.getCustomerGroups()).isEmpty();
        assertThat(response.getTimeBuckets()).isEmpty();
    }

    @Test
    @DisplayName("from() should map PlanBoardView aggregates and plan cards")
    void shouldMapPlanBoardViewToDto() {
        OffsetDateTime start = OffsetDateTime.parse("2024-05-01T08:00:00+08:00");
        OffsetDateTime end = start.plusHours(2);
        PlanBoardView.PlanCard planCard = new PlanBoardView.PlanCard(
                "plan-dto-1",
                "多视图计划",
                PlanStatus.IN_PROGRESS,
                "dto-owner",
                "cust-dto",
                start,
                end,
                "Asia/Shanghai",
                65,
                true,
                false,
                null,
                90L
        );
        PlanBoardView.CustomerGroup group = new PlanBoardView.CustomerGroup(
                "cust-dto",
                "客户DTO",
                3,
                2,
                1,
                1,
                1,
                2,
                47.5,
                start,
                end,
                List.of(planCard)
        );
        PlanBoardView.TimeBucket bucket = new PlanBoardView.TimeBucket(
                "2024-05-01",
                start,
                end,
                3,
                2,
                1,
                1,
                1,
                2,
                List.of(planCard)
        );
        PlanBoardView.Metrics metrics = new PlanBoardView.Metrics(
                3,
                2,
                1,
                1,
                1,
                2,
                47.5,
                2.5,
                50.0
        );
        OffsetDateTime reference = OffsetDateTime.parse("2024-06-03T10:00:00Z");
        PlanBoardView view = new PlanBoardView(List.of(group), List.of(bucket), metrics, PlanBoardGrouping.WEEK, reference);

        PlanBoardResponse response = PlanBoardResponse.from(view);

        assertThat(response.getGranularity()).isEqualTo(PlanBoardGrouping.WEEK.name());
        assertThat(response.getReferenceTime()).isEqualTo(reference);
        assertThat(response.getMetrics().getCompletionRate()).isEqualTo(50.0);
        assertThat(response.getMetrics().getAverageDurationHours()).isEqualTo(2.5);
        assertThat(response.getCustomerGroups()).hasSize(1);
        PlanBoardResponse.CustomerGroupResponse customer = response.getCustomerGroups().get(0);
        assertThat(customer.getCustomerId()).isEqualTo("cust-dto");
        assertThat(customer.getCustomerName()).isEqualTo("客户DTO");
        assertThat(customer.getAtRiskPlans()).isEqualTo(2);
        assertThat(customer.getPlans()).hasSize(1);
        PlanBoardResponse.PlanCardResponse dtoCard = customer.getPlans().get(0);
        assertThat(dtoCard.getStatus()).isEqualTo(PlanStatus.IN_PROGRESS.name());
        assertThat(dtoCard.getMinutesOverdue()).isEqualTo(90L);
        assertThat(response.getTimeBuckets()).hasSize(1);
        assertThat(response.getTimeBuckets().get(0).getPlans()).extracting(PlanBoardResponse.PlanCardResponse::getId)
                .containsExactly("plan-dto-1");
        assertThat(response.getTimeBuckets().get(0).getAtRiskPlans()).isEqualTo(2);
        assertThat(response.getMetrics().getAtRiskPlans()).isEqualTo(2);
    }

    @Test
    @DisplayName("PlanBoardResponse collections should be unmodifiable for callers")
    void shouldExposeUnmodifiableCollections() {
        OffsetDateTime now = OffsetDateTime.parse("2024-07-01T00:00:00Z");
        PlanBoardView.PlanCard planCard = new PlanBoardView.PlanCard(
                "plan-unmodifiable",
                "受保护计划",
                PlanStatus.SCHEDULED,
                "owner-unmodifiable",
                "customer-unmodifiable",
                now,
                now.plusHours(1),
                "UTC",
                10,
                false,
                false,
                null,
                null
        );
        PlanBoardView.CustomerGroup group = new PlanBoardView.CustomerGroup(
                "customer-unmodifiable",
                null,
                1,
                1,
                0,
                0,
                0,
                0,
                10.0,
                now,
                now.plusHours(1),
                List.of(planCard)
        );
        PlanBoardView.TimeBucket bucket = new PlanBoardView.TimeBucket(
                "2024-07-01",
                now,
                now.plusDays(1),
                1,
                1,
                0,
                0,
                0,
                0,
                List.of(planCard)
        );
        PlanBoardView view = new PlanBoardView(List.of(group), List.of(bucket),
                new PlanBoardView.Metrics(1, 1, 0, 0, 0, 0, 10.0, 1.0, 0.0), PlanBoardGrouping.DAY, now);

        PlanBoardResponse response = PlanBoardResponse.from(view);

        assertThatThrownBy(() -> response.getCustomerGroups().add(null)).isInstanceOf(UnsupportedOperationException.class);
        PlanBoardResponse.CustomerGroupResponse customerResponse = response.getCustomerGroups().get(0);
        assertThatThrownBy(() -> customerResponse.getPlans().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> response.getTimeBuckets().add(null)).isInstanceOf(UnsupportedOperationException.class);
        PlanBoardResponse.TimeBucketResponse bucketResponse = response.getTimeBuckets().get(0);
        assertThatThrownBy(() -> bucketResponse.getPlans().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }
}
