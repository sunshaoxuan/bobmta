package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.repository.PlanBoardGrouping;
import com.bob.mta.modules.plan.service.PlanBoardView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanBoardResponseTest {

    @Test
    @DisplayName("from(null) should create empty board response with zero metrics")
    void shouldCreateEmptyResponseWhenViewMissing() {
        PlanBoardResponse response = PlanBoardResponse.from(null);

        assertThat(response.getCustomerGroups()).isEmpty();
        assertThat(response.getTimeBuckets()).isEmpty();
        assertThat(response.getMetrics()).isNotNull();
        assertThat(response.getMetrics().getTotalPlans()).isZero();
        assertThat(response.getMetrics().getActivePlans()).isZero();
        assertThat(response.getMetrics().getAtRiskPlans()).isZero();
        assertThat(response.getGranularity()).isEqualTo(PlanBoardGrouping.WEEK.name());
        assertThat(response.getReferenceTime()).isNull();
    }

    @Test
    @DisplayName("from(view) should copy customer groups, time buckets and metrics")
    void shouldMapNestedStructures() {
        OffsetDateTime reference = OffsetDateTime.of(2024, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime start = reference.plusHours(1);
        OffsetDateTime end = reference.plusHours(3);

        PlanBoardView.PlanCard planCard = new PlanBoardView.PlanCard(
                "PLAN-9001",
                "多视图计划",
                PlanStatus.SCHEDULED,
                "owner-id",
                "cust-id",
                start,
                end,
                "Asia/Shanghai",
                65,
                true,
                false,
                null,
                30L
        );

        PlanBoardView.CustomerGroup customerGroup = new PlanBoardView.CustomerGroup(
                "cust-id",
                "客户名称",
                3,
                2,
                1,
                1,
                1,
                2,
                start,
                end,
                List.of(planCard)
        );

        PlanBoardView.TimeBucket bucket = new PlanBoardView.TimeBucket(
                "2024-07-01",
                start,
                end,
                2,
                1,
                1,
                1,
                0,
                1,
                List.of(planCard)
        );

        PlanBoardView.Metrics metrics = new PlanBoardView.Metrics(
                5,
                2,
                1,
                1,
                1,
                2,
                45.5,
                3.5,
                20.0
        );

        PlanBoardView view = new PlanBoardView(
                List.of(customerGroup),
                List.of(bucket),
                metrics,
                PlanBoardGrouping.DAY,
                reference
        );

        PlanBoardResponse response = PlanBoardResponse.from(view);

        assertThat(response.getGranularity()).isEqualTo("DAY");
        assertThat(response.getReferenceTime()).isEqualTo(reference);
        assertThat(response.getMetrics().getTotalPlans()).isEqualTo(5);
        assertThat(response.getMetrics().getAtRiskPlans()).isEqualTo(2);
        assertThat(response.getMetrics().getAverageProgress()).isEqualTo(45.5);
        assertThat(response.getCustomerGroups()).hasSize(1);

        PlanBoardResponse.CustomerGroupResponse groupResponse = response.getCustomerGroups().get(0);
        assertThat(groupResponse.getCustomerId()).isEqualTo("cust-id");
        assertThat(groupResponse.getCustomerName()).isEqualTo("客户名称");
        assertThat(groupResponse.getPlans()).hasSize(1);
        assertThat(groupResponse.getAtRiskPlans()).isEqualTo(2);

        PlanBoardResponse.TimeBucketResponse bucketResponse = response.getTimeBuckets().get(0);
        assertThat(bucketResponse.getBucketId()).isEqualTo("2024-07-01");
        assertThat(bucketResponse.getPlans()).extracting(PlanBoardResponse.PlanCardResponse::getId)
                .containsExactly("PLAN-9001");
        assertThat(bucketResponse.getAtRiskPlans()).isEqualTo(1);

        PlanBoardResponse.PlanCardResponse cardResponse = groupResponse.getPlans().get(0);
        assertThat(cardResponse.getStatus()).isEqualTo(PlanStatus.SCHEDULED.name());
        assertThat(cardResponse.getMinutesOverdue()).isEqualTo(30L);
        assertThat(cardResponse.isOverdue()).isTrue();
    }

    @Test
    @DisplayName("from(view) should fall back to zero metrics when aggregate missing")
    void shouldFallbackToZeroMetricsWhenViewOmitsAggregates() {
        OffsetDateTime reference = OffsetDateTime.of(2024, 8, 15, 0, 0, 0, 0, ZoneOffset.UTC);
        PlanBoardView.PlanCard planCard = new PlanBoardView.PlanCard(
                "PLAN-9100",
                "缺省指标计划",
                PlanStatus.IN_PROGRESS,
                "owner-metrics",
                "cust-metrics",
                reference.plusHours(2),
                reference.plusHours(5),
                "Asia/Shanghai",
                50,
                false,
                true,
                120L,
                null
        );

        PlanBoardView.CustomerGroup group = new PlanBoardView.CustomerGroup(
                "cust-metrics",
                null,
                1,
                1,
                0,
                0,
                1,
                1,
                reference.plusHours(2),
                reference.plusHours(5),
                List.of(planCard)
        );

        PlanBoardView.TimeBucket bucket = new PlanBoardView.TimeBucket(
                "2024-08-15",
                reference,
                reference.plusDays(1),
                1,
                1,
                0,
                0,
                1,
                1,
                List.of(planCard)
        );

        PlanBoardView view = new PlanBoardView(
                List.of(group),
                List.of(bucket),
                null,
                PlanBoardGrouping.DAY,
                reference
        );

        PlanBoardResponse response = PlanBoardResponse.from(view);

        assertThat(response.getMetrics().getTotalPlans()).isZero();
        assertThat(response.getMetrics().getAverageDurationHours()).isZero();
        assertThat(response.getGranularity()).isEqualTo("DAY");
        assertThat(response.getCustomerGroups()).singleElement()
                .extracting(PlanBoardResponse.CustomerGroupResponse::getTotalPlans)
                .isEqualTo(1L);
        assertThat(response.getTimeBuckets()).singleElement()
                .extracting(PlanBoardResponse.TimeBucketResponse::getBucketId)
                .isEqualTo("2024-08-15");
    }
}

