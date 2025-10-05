# 计划统计驾驶舱数据接�?

## 背景
前端驾驶舱页面需要在单个接口中获取计划状态总览、负责人负载情况以及即将到期/已逾期计划的风险提示，便于构建状态图表与提醒卡片。本说明用于补充 `GET /api/v1/plans/analytics` 的最新响应结构与字段含义�?

## 请求
- **Method**: `GET`
- **Path**: `/api/v1/plans/analytics`
- **Query**:
  - `tenantId` *(optional)*：指定租户进行隔离；为空时返回当前环境全部计划概览�?
  - `customerId` *(optional)*：按客户过滤分析结果�?
  - `ownerId` *(optional)*：限定特定负责人范围，统计结果仅包含该负责人负责的计划�?
  - `from`、`to` *(optional, ISO-8601 datetime)*：筛选计划预计时间窗的开�?结束边界�?

## 响应
> ��������������`backend/src/test/resources/fixtures/plan-analytics-baseline.json` ������ǰ����������նԱȡ�

```jsonc
{
  "totalPlans": 42,
  "designCount": 3,
  "scheduledCount": 11,
  "inProgressCount": 15,
  "completedCount": 10,
  "canceledCount": 3,
  "overdueCount": 4,
  "upcomingPlans": [
    {
      "id": "plan-1001",
      "title": "东京数据中心巡检",
      "status": "SCHEDULED",
      "plannedStartTime": "2024-04-02T01:00:00+09:00",
      "plannedEndTime": "2024-04-02T05:00:00+09:00",
      "owner": "alice",
      "customerId": "cust-01",
      "progress": 20
    }
  ],
  "ownerLoads": [
    {
      "ownerId": "alice",
      "totalPlans": 8,
      "activePlans": 6,
      "overduePlans": 2
    }
  ],
  "riskPlans": [
    {
      "id": "plan-0995",
      "title": "香港IDC 机房复检",
      "status": "IN_PROGRESS",
      "plannedEndTime": "2024-04-01T18:00:00+08:00",
      "owner": "bob",
      "customerId": "cust-05",
      "riskLevel": "OVERDUE",
      "minutesUntilDue": 0,
      "minutesOverdue": 180
    },
    {
      "id": "plan-1010",
      "title": "上海机房例行保养",
      "status": "SCHEDULED",
      "plannedEndTime": "2024-04-02T09:30:00+08:00",
      "owner": "carol",
      "customerId": "cust-03",
      "riskLevel": "DUE_SOON",
      "minutesUntilDue": 120,
      "minutesOverdue": 0
    }
  ]
}
```

### 字段说明
- `ownerLoads`：最多返�?5 位负责人，按活跃计划数倒序排列；`activePlans` 包含 `SCHEDULED/IN_PROGRESS` 状态，`overduePlans` 代表�?24 小时窗口内已逾期的活跃计划。传�?`ownerId` 时，仅返回对应负责人的统计�?
- `riskPlans`：最多返�?5 条风险计划，优先展示 `OVERDUE`，随后是未来 24 小时�?`DUE_SOON` 的计划；`minutesUntilDue` �?`minutesOverdue` 均为非负整数，单位分钟�?

## 校验与验�?
- 通过租户、客户或负责人过滤时，`ownerLoads` �?`riskPlans` 仅统计过滤范围内的数据�?
- 新增服务层与控制层单元测试覆盖逾期及即将到期场景；前端可使用上述样例对齐卡片渲染与风险提醒逻辑�?

## 交付状�?
- �?后端已实�?`GET /api/v1/plans/analytics` 并补充驾驶舱统计与风险计划逻辑，当前接口已在服务层集成过滤条件与阈值校验�?
- 🔄 前端待完成驾驶舱页面的接口接入与图表渲染联调�?
