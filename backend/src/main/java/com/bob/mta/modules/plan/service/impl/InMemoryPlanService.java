package com.bob.mta.modules.plan.service.impl;

<<<<<<< HEAD
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.plan.domain.Plan;
import com.bob.mta.modules.plan.domain.PlanNode;
import com.bob.mta.modules.plan.domain.PlanStatus;
import com.bob.mta.modules.plan.service.PlanService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class InMemoryPlanService implements PlanService {

    private final ConcurrentMap<String, Plan> plans = new ConcurrentHashMap<>();

    public InMemoryPlanService() {
        seedPlans();
    }

    private void seedPlans() {
        PlanNode backupDb = new PlanNode("node-1", "数据库备份", "REMOTE", "admin", 1, List.of());
        PlanNode notify = new PlanNode("node-2", "通知客户", "EMAIL", "operator", 2, List.of());
        Plan plan1 = new Plan(
                "plan-001",
                "东京医疗季度巡检",
                "cust-001",
                "admin",
                PlanStatus.SCHEDULED,
                OffsetDateTime.now().plusDays(3),
                OffsetDateTime.now().plusDays(3).plusHours(4),
                30,
                List.of(backupDb, notify)
        );
        PlanNode review = new PlanNode("node-3", "现场巡检", "CHECKLIST", "operator", 1, List.of());
        Plan plan2 = new Plan(
                "plan-002",
                "大阪制造系统升级",
                "cust-002",
                "operator",
                PlanStatus.DESIGN,
                OffsetDateTime.now().plusWeeks(1),
                OffsetDateTime.now().plusWeeks(1).plusHours(6),
                10,
                List.of(review)
        );
        plans.put(plan1.getId(), plan1);
        plans.put(plan2.getId(), plan2);
    }

    @Override
    public List<Plan> listPlans(String customerId, String status) {
        return plans.values().stream()
                .filter(plan -> !StringUtils.hasText(customerId) || plan.getCustomerId().equals(customerId))
                .filter(plan -> !StringUtils.hasText(status) || plan.getStatus().name().equalsIgnoreCase(status))
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());
    }

    @Override
    public Plan getPlan(String id) {
        Plan plan = plans.get(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
        return plan;
=======
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.plan.dto.PlanDetailResponse;
import com.bob.mta.modules.plan.dto.PlanNodeResponse;
import com.bob.mta.modules.plan.dto.PlanSummaryResponse;
import com.bob.mta.modules.plan.service.PlanService;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * In-memory plan service describing the execution/ design lifecycle endpoints.
 */
@Service
public class InMemoryPlanService implements PlanService {

    private final Map<String, PlanDetailResponse> plans;

    public InMemoryPlanService() {
        plans = Map.of(
                "PLAN-5001",
                new PlanDetailResponse(
                        "PLAN-5001",
                        "101",
                        "北海道大学 VPN 定期メンテナンス",
                        "VPNクライアント証明書更新と接続確認を実施",
                        Instant.parse("2024-05-10T00:00:00Z"),
                        Instant.parse("2024-05-10T04:00:00Z"),
                        "IN_PROGRESS",
                        List.of("admin", "operator"),
                        List.of(
                                new PlanNodeResponse(
                                        "NODE-1",
                                        "事前バックアップ取得",
                                        "BACKUP",
                                        "DONE",
                                        Duration.ofMinutes(30),
                                        Instant.parse("2024-05-10T00:00:00Z"),
                                        Instant.parse("2024-05-10T00:40:00Z"),
                                        List.of("admin")),
                                new PlanNodeResponse(
                                        "NODE-2",
                                        "VPN証明書更新",
                                        "OPERATION",
                                        "IN_PROGRESS",
                                        Duration.ofMinutes(60),
                                        Instant.parse("2024-05-10T01:00:00Z"),
                                        null,
                                        List.of("operator")),
                                new PlanNodeResponse(
                                        "NODE-3",
                                        "接続確認・報告",
                                        "VERIFY",
                                        "PENDING",
                                        Duration.ofMinutes(30),
                                        null,
                                        null,
                                        List.of("operator")))),
                "PLAN-5100",
                new PlanDetailResponse(
                        "PLAN-5100",
                        "201",
                        "东京メトロ 回線切替リハーサル",
                        "回線切替手順の検証とスクリプト更新",
                        Instant.parse("2024-05-15T02:00:00Z"),
                        Instant.parse("2024-05-15T06:00:00Z"),
                        "DESIGN",
                        List.of("admin"),
                        List.of(
                                new PlanNodeResponse(
                                        "NODE-1",
                                        "手順確認ミーティング",
                                        "MEETING",
                                        "PENDING",
                                        Duration.ofMinutes(45),
                                        null,
                                        null,
                                        List.of("admin")),
                                new PlanNodeResponse(
                                        "NODE-2",
                                        "切替スクリプト修正",
                                        "DOCUMENT",
                                        "PENDING",
                                        Duration.ofMinutes(90),
                                        null,
                                        null,
                                        List.of("admin")))));
    }

    @Override
    public PageResponse<PlanSummaryResponse> listPlans(
            final int page, final int pageSize, final String customerId, final String status) {
        final List<PlanDetailResponse> filtered = plans.values().stream()
                .filter(plan -> filterByCustomer(plan, customerId))
                .filter(plan -> filterByStatus(plan, status))
                .sorted(Comparator.comparing(PlanDetailResponse::getStartTime))
                .toList();
        final int fromIndex = Math.max(0, Math.min(filtered.size(), (page - 1) * pageSize));
        final int toIndex = Math.max(fromIndex, Math.min(filtered.size(), fromIndex + pageSize));
        final List<PlanSummaryResponse> pageData = filtered.subList(fromIndex, toIndex).stream()
                .map(plan -> new PlanSummaryResponse(
                        plan.getId(),
                        plan.getCustomerId(),
                        plan.getTitle(),
                        plan.getStartTime(),
                        plan.getEndTime(),
                        plan.getStatus(),
                        plan.getAssignees(),
                        plan.getNodes().size(),
                        (int) plan.getNodes().stream().filter(node -> "DONE".equals(node.getStatus())).count()))
                .collect(Collectors.toList());
        return PageResponse.of(pageData, filtered.size(), page, pageSize);
    }

    @Override
    public PlanDetailResponse getPlan(final String id) {
        return plans.entrySet().stream()
                .filter(entry -> entry.getKey().equals(id))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "plan.not_found"));
    }

    private boolean filterByCustomer(final PlanDetailResponse plan, final String customerId) {
        if (!StringUtils.hasText(customerId)) {
            return true;
        }
        return customerId.equalsIgnoreCase(plan.getCustomerId());
    }

    private boolean filterByStatus(final PlanDetailResponse plan, final String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return status.equalsIgnoreCase(plan.getStatus());
>>>>>>> origin/main
    }
}
