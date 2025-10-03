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
  - 所有筛选条件将汇总到 `PlanSearchCriteria` 中，由服务层统一传递至持久层执行聚合。
  - 空字符串与重复的 `customerId` / `status` 会在控制层被自动去重、裁剪，避免生成冗余的筛选条件与审计快照。

## 响应

```jsonc
{
  "granularity": "DAY",
  "referenceTime": "2024-04-02T00:00:00Z",
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
- `referenceTime`：派生指标计算时采用的参考时间（UTC），前端可用来判断倒计时差异或触发刷新。
- `metrics.averageProgress`：所有命中计划的平均执行进度，保留 1 位小数。
- `metrics.averageDurationHours`：根据 `plannedStartTime` 与 `plannedEndTime` 计算的平均计划时长（小时）。
- `metrics.dueSoonPlans`：在 24 小时内即将到期的活跃计划数量，结合 `overduePlans` 便于判定风险分布。
- `metrics.completionRate`：已完成计划的百分比（0-100），保留 1 位小数。
- `customerGroups`：按 `customerId` 聚合的分组信息，包含活跃/完成数量、时间窗口范围以及计划卡片。
- 当计划缺失客户编号时，`customerId` 会被折叠为 `UNKNOWN`，仍可通过分组下的卡片访问原始计划。
- `timeBuckets`：按粒度拆分的时间桶，`bucketId` 作为前端 Tab/日历的 key，`plans` 用于快速渲染对应视图。
- `plans[].overdue` / `plans[].dueSoon`：派生风险指标，分别表示计划已逾期或在默认阈值内即将到期。
- `plans[].minutesUntilDue` / `plans[].minutesOverdue`：结合风险标识的分钟粒度倒计时，便于前端展示剩余时间或逾期时长。
- 当未命中任何计划时，`metrics` 字段依然会返回各项值为 `0` 的对象，避免前端处理空指针并保持界面指标稳定。

## 验收说明

- 当同时传入多个 `customerId` 时，结果只保留在列表中的客户，同时更新时间桶统计。
- `status` 过滤与 `GET /api/v1/plans` 保持一致，默认返回全部状态。
- 已提供控制层与服务层单元测试覆盖不同租户/客户的组合，以支撑多租户环境下的联调。
- 新增 `PlanPersistenceAnalyticsRepositoryTest` 中的持久化层集成测试，校验 SQL 聚合在多客户筛选、跨租户过滤及空结果场景下与内存实现保持一致。
- 每次调用会写入 `PlanBoard` 的审计快照，包含查询租户范围与响应摘要，便于追踪驾驶舱访问行为。

## 交付状态

- ✅ 后端已在 `PlanService#getPlanBoard` 与 `PlanController#board` 提供实现，基于 `PlanSearchCriteria` 聚合客户/时间桶并记录审计快照，补充 SQL 聚合与未知客户分组逻辑及单测、示例响应。
- ✅ 前端 `PlanListBoard` 已提供 `Segmented` 视图切换，并通过 `PlanByCustomerView`、`PlanCalendarView` 显示客户分组与多粒度日历。派生逻辑在 `planList` 状态中复用后端返回的数据，缺失时会本地聚合以保持体验一致。
- ✅ 视图模式会同步至 URL 查询参数，刷新或分享页面时会自动还原 `Segmented`/`Tabs` 的选项，避免用户重复筛选。
- ✅ 客户视图与日历视图会按计划时间排序展示卡片，新增的前端单元测试验证客户分组与时间桶聚合的稳定性，保证多视图切换时的顺序一致。
