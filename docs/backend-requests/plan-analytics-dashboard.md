# 计划统计驾驶舱数据接口

## 背景
前端驾驶舱页面需要在单个接口中获取计划状态总览、负责人负载情况以及即将到期/已逾期计划的风险提示，便于构建状态图表与提醒卡片。本说明用于补充 `GET /api/v1/plans/analytics` 的最新响应结构与字段含义。

## 请求
- **Method**: `GET`
- **Path**: `/api/v1/plans/analytics`
- **Query**:
  - `tenantId` *(optional)*：指定租户进行隔离；为空时返回当前环境全部计划概览。
  - `customerId` *(optional)*：按客户过滤分析结果。
  - `ownerId` *(optional)*：限定特定负责人范围，统计结果仅包含该负责人负责的计划。
  - `from`、`to` *(optional, ISO-8601 datetime)*：筛选计划预计时间窗的开始/结束边界。
  - `referenceTime` *(optional, ISO-8601 datetime)*：统计基准时间，默认取调用瞬间；可用于回放历史快照或对齐前端缓存刷新时间。
  - `upcomingLimit` *(optional, integer > 0)*：即将开始计划列表的最大条目数，默认 5。
  - `ownerLimit` *(optional, integer > 0)*：负责人负载列表的最大条目数，默认 5。
  - `riskLimit` *(optional, integer > 0)*：风险计划列表的最大条目数，默认 5。
  - `dueSoonMinutes` *(optional, integer > 0)*：将计划归类为「即将到期」的时间窗口，单位分钟，默认 1440 分钟（24 小时）。

## 响应
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
- `ownerLoads`：默认返回 5 位负责人，按活跃计划数倒序排列；可通过 `ownerLimit` 调整数量上限。`activePlans` 包含 `SCHEDULED/IN_PROGRESS` 状态，`overduePlans` 代表在 24 小时窗口内已逾期的活跃计划。传入 `ownerId` 时，仅返回对应负责人的统计。
- `riskPlans`：默认返回 5 条风险计划，优先展示 `OVERDUE`，随后是未来 24 小时内 `DUE_SOON` 的计划；可通过 `riskLimit` 调整数量上限。`minutesUntilDue` 与 `minutesOverdue` 均为非负整数，单位分钟。
  - 若 `dueSoonMinutes` 指定小于 24 小时的窗口，则仅会返回在该窗口内即将到期的计划。
  - 当指定 `riskLimit` 时，最多返回相应数量的风险计划。
  - 指定 `referenceTime` 可确保「即将到期」计算基于同一时间基准，便于前端缓存命中时重放快照。

## 校验与验收
- 通过租户、客户或负责人过滤时，`ownerLoads` 与 `riskPlans` 仅统计过滤范围内的数据。
- 新增服务层与控制层单元测试覆盖逾期及即将到期场景；前端可使用上述样例对齐卡片渲染与风险提醒逻辑。
