# 计划多视图驾驶舱接口

## 背景

前端 `PlanListBoard` 页面需要在单次调用中获取客户分组、时间桶统计以及派生指标，用于驱动分段列表、日/周视图和概览卡片。本说明同步 `GET /api/v1/plans/board` 的契约与示例。

## 请求

- **Method**: `GET`
- **Path**: `/api/v1/plans/board`
- **Query**:
  - `tenantId` *(optional)*：指定租户范围，未提供则返回所有租户的计划。
  - `customerId` *(optional, repeatable)*：过滤客户，支持多选；当传入多个值时仅保留命中的客户分组。
  - `owner` *(optional)*：按负责人过滤。
  - `status` *(optional, repeatable)*：筛选计划状态，示例：`status=SCHEDULED&status=IN_PROGRESS`。
  - `from` / `to` *(optional, ISO-8601 datetime)*：限制计划预计时间窗的上下界。
  - `granularity` *(optional, enum)*：时间桶粒度，支持 `DAY`/`WEEK`/`MONTH`/`YEAR`，默认 `WEEK`。

## 响应

```jsonc
{
  "granularity": "DAY",
  "metrics": {
    "totalPlans": 2,
    "activePlans": 2,
    "completedPlans": 0,
    "overduePlans": 1,
    "dueSoonPlans": 1,
    "averageProgress": 45.5,
    "averageDurationHours": 2.5,
    "completionRate": 0.0
  },
  "customerGroups": [
    {
      "customerId": "cust-board-1",
      "customerName": null,
      "totalPlans": 1,
      "activePlans": 1,
      "completedPlans": 0,
      "overduePlans": 0,
      "dueSoonPlans": 1,
      "averageProgress": 30.0,
      "earliestStart": "2024-04-02T08:00:00+08:00",
      "latestEnd": "2024-04-02T10:30:00+08:00",
      "plans": [
        {
          "id": "PLAN-6001",
          "title": "控制层看板计划A",
          "status": "SCHEDULED",
          "owner": "controller-board-owner",
          "customerId": "cust-board-1",
          "plannedStartTime": "2024-04-02T08:00:00+08:00",
          "plannedEndTime": "2024-04-02T10:30:00+08:00",
          "timezone": "Asia/Shanghai",
          "progress": 30,
          "overdue": false,
          "dueSoon": true,
          "minutesUntilDue": 90,
          "minutesOverdue": null
        }
      ]
    }
  ],
  "timeBuckets": [
    {
      "bucketId": "2024-04-02",
      "start": "2024-04-02T00:00:00+08:00",
      "end": "2024-04-03T00:00:00+08:00",
      "totalPlans": 1,
      "activePlans": 1,
      "completedPlans": 0,
      "overduePlans": 0,
      "dueSoonPlans": 1,
      "plans": [
        {
          "id": "PLAN-6001",
          "title": "控制层看板计划A",
          "status": "SCHEDULED",
          "owner": "controller-board-owner",
          "customerId": "cust-board-1",
          "plannedStartTime": "2024-04-02T08:00:00+08:00",
          "plannedEndTime": "2024-04-02T10:30:00+08:00",
          "timezone": "Asia/Shanghai",
          "progress": 30,
          "overdue": false,
          "dueSoon": true,
          "minutesUntilDue": 90,
          "minutesOverdue": null
        }
      ]
    }
  ]
}
```

### 字段说明

- `granularity`：表示当前响应采用的时间粒度，取值为 `DAY`/`WEEK`/`MONTH`/`YEAR`。
- `metrics.averageProgress`：所有命中计划的平均执行进度，保留 1 位小数。
- `metrics.averageDurationHours`：根据 `plannedStartTime` 与 `plannedEndTime` 计算的平均计划时长（小时）。
- `metrics.dueSoonPlans`：在 24 小时内即将到期的活跃计划数量，结合 `overduePlans` 便于判定风险分布。
- `metrics.completionRate`：已完成计划的百分比（0-100），保留 1 位小数。
- `customerGroups`：按 `customerId` 聚合的分组信息，包含活跃/完成数量、时间窗口范围以及计划卡片。
- `timeBuckets`：按粒度拆分的时间桶，`bucketId` 作为前端 Tab/日历的 key，`plans` 用于快速渲染对应视图。
- `plans[].overdue` / `plans[].dueSoon`：派生风险指标，分别表示计划已逾期或在默认阈值内即将到期。
- `plans[].minutesUntilDue` / `plans[].minutesOverdue`：结合风险标识的分钟粒度倒计时，便于前端展示剩余时间或逾期时长。

## 验收说明

- 当同时传入多个 `customerId` 时，结果只保留在列表中的客户，同时更新时间桶统计。
- `status` 过滤与 `GET /api/v1/plans` 保持一致，默认返回全部状态。
- 已提供控制层与服务层单元测试覆盖不同租户/客户的组合，以支撑多租户环境下的联调。

## 交付状态

- ✅ 后端已在 `PlanService#getPlanBoard` 与 `PlanController#board` 提供实现，并补充单测与示例响应。
- 🔄 前端可依据本文档调整 `PlanListBoard` 的请求逻辑与数据映射。
