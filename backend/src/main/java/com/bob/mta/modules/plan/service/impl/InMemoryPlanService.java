package com.bob.mta.modules.plan.service.impl;

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
    }
}
