# 前端需求清单

本清单用于跟踪前端在各迭代对后端接口、数据与配置的需求，同时记录 Mock 策略与联调结果。状态字段建议使用以下标记：
- `🟡 规划中`
- `🛠️ 开发中`
- `🧪 联调中`
- `✅ 完成`
- `⚠️ 阻塞`

## 管理约束
1. 所有新增需求需在提交代码前于本文件创建条目，并在 PR 描述中引用编号。
2. 若需求涉及接口调整，请在 `docs/backend-requests/` 下新增说明文档，包含场景描述、接口契约草案、数据范围与验收方式。
3. 后端完成实现后，应在「后端状态」列更新为 `✅ 完成` 或 `🧪 联调中` 并附带接口文档/示例；前端完成联调后再更新「前端状态」。
4. 未完成的后端能力，前端必须在「Mock 策略」列说明模拟数据方案及覆盖的测试范围。
5. 提交涉及第三方库的需求前，需确认是否能在离线环境复用现有 `vendor/` 组件或新增离线镜像方案，避免再次触发安装失败。

## 需求列表
| 编号 | 功能场景 | 接口需求 | 数据范围/示例 | 后端状态 | 前端状态 | Mock 策略 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| F-000 | 登录与计划列表基线 | `POST /api/v1/auth/login`、`GET /api/v1/plans?page=0&size=20` | 计划列表需返回 `PageResponse<PlanSummary>`，包含进度、参与人数量等字段 | ✅ 完成 | ✅ 完成（迭代 #0） | 未使用 Mock；直接联调 | 继续关注分页参数及多语言头的兼容性；同步完成导航菜单角色过滤与 403 提示 |
| F-001 | 计划列表筛选扩展 | `GET /api/v1/plans` 支持负责人、关键字、时间范围与分页参数 | 需确认分页上限、关键字匹配策略及排序顺序 | ✅ 完成（`GET /api/v1/plans/filter-options` 已在 2025-09 发布，契约详见《[计划列表筛选字典接口说明](../docs/backend-requests/plan-filter-options.md)》，示例：`GET /api/v1/plans/filter-options?tenantId=acme`） | 🟡 规划中 | 保留 `queryMockPlanSummaries` 过滤分页样例覆盖 owner/keyword/status/page/size，并以 `mockPlanFilterOptions.json` 镜像 `statusLabel`、`ownerLabel`、`customerLabel`、`plannedWindow`（含 `label`、`hint` 与 `start/end` 可空）等字段，确保 Mock 数量与排序规则与后端一致后再切换真实接口 | 筛选字典接口将用于替换静态枚举，前端接入时同步落地缓存刷新/回退策略并补充查询参数校验 |
| F-002 | 计划详情与时间线视图 | `GET /api/v1/plans/{id}`、`GET /api/v1/plans/{id}/timeline` | 计划节点、附件、时间线事件的字段需确认必填项 | ✅ 完成（`GET /api/v1/plans/activity-types` 已在 2025-09 发布，契约详见《[计划时间线事件字典说明](../docs/backend-requests/plan-timeline-activities.md)》，多语言响应示例已在文档补充） | 🛠️ 开发中 | 继续使用计划详情/时间线/提醒 Mock 样例驱动组件开发，追加 `mockPlanActivityTypes.json` 注入 `type`、`messages[key/message]`、`attributes[name/descriptionKey/description]` 字段并以单测覆盖图标映射与属性描述渲染 | 前端将基于字典接口渲染时间线事件与属性说明，并对接真实接口后的缓存与降级策略 |
| F-003 | 提醒策略配置 | `GET /api/v1/plans/{id}/reminders`、`PUT /api/v1/plans/{id}/reminders`、`GET /api/v1/plans/reminder-options` | 提醒渠道、触发时机、模板 ID 等字段 | ✅ 完成 | 🟡 规划中 | 设计默认策略样例及更新成功响应 | 后端提供提醒配置字典，详见《docs/backend-requests/plan-reminder-options.md》 |
| F-004 | 计划统计驾驶舱 | `GET /api/v1/plans/analytics` | 按状态、负责人、逾期风险等聚合数据 | 🧪 联调中 | 🟡 规划中 | 参考阶段三文档构造统计 Mock | 新增负责人负载与风险计划字段，详见《docs/backend-requests/plan-analytics-dashboard.md》；接口支持 `ownerId` 查询参数便于聚焦单个负责人 |
| F-005 | 节点执行与提醒控制 | `POST /api/v1/plans/{id}/nodes/{nodeId}/{action}`、`PUT /api/v1/plans/{id}/reminders/{reminderId}` | 需返回最新 `PlanDetailPayload`（节点、时间线、提醒） | ✅ 完成 | 🛠️ 开发中 | Mock 将在下个迭代替换为真实接口联调 | 详见《docs/backend-requests/plan-node-operations.md》，后端已交付节点开始/完成/交接与提醒规则更新接口，并新增 `actionType`/`completionThreshold` 字段及阈值自动跳过逻辑 |
| F-006 | 计划多视图驾驶舱 | `GET /api/v1/plans/board` 提供客户信息、计划时间窗 | 需要返回客户标识/名称及计划窗口（开始/结束）字段 | ✅ 完成（SQL 聚合 & UNKNOWN 客户兜底） | ✅ 完成（迭代 #2 视图扩展） | 扩展 `listMockPlans` 与 `planDetail` 样例补充客户字段，封装 `PlanByCustomerView`/`PlanCalendarView` 组件消费聚合结果，派生客户分组与日历事件并以 Node Test 校验排序、时间桶起止与时长计算 | 新增 `GET /api/v1/plans/board` 接口及《docs/backend-requests/plan-board-view.md》，支持客户分组、时间桶、进度与风险派生指标，新增 SQL 聚合保障多租户排序一致，前端可替换 Mock 请求 |

> 当条目状态发生变化时，请同步更新根目录 README 的「前端阶段四迭代进度」摘要。
