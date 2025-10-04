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
| F-001 | 计划列表筛选扩展 | `GET /api/v1/plans` 支持负责人、关键字、时间范围与分页参数 | 需确认分页上限、关键字匹配策略及排序顺序 | ✅ 完成（2025-09-29 起生产环境提供 `GET /api/v1/plans/filter-options`，2025-10-02 与后端复核持续可用；示例 `curl` 详见《[计划列表筛选字典接口说明](../docs/backend-requests/plan-filter-options.md#示例请求)》） | 🧪 联调中（接入真实筛选字典并验证分页与缓存回退策略） | 默认命中生产接口 `GET /api/v1/plans/filter-options` 并记录 `ETag`/`Last-Modified`；`queryMockPlanSummaries` 与 `mockPlanFilterOptions.json` 仅在离线/测试环境或接口异常时兜底，持续通过 Node Test 校验与线上契约一致；真实接口返回 `304 Not Modified` 时按文档回退本地缓存并记录刷新时间 | 筛选字典接口替换静态枚举中，需补齐 `tenantId` 透传、筛选面板的多语言提示校验及缓存刷新审计日志 |
| F-002 | 计划详情与时间线视图 | `GET /api/v1/plans/{id}`、`GET /api/v1/plans/{id}/timeline` | 计划节点、附件、时间线事件的字段需确认必填项 | ✅ 完成（2025-09-29 起生产环境已上线时间线事件字典 `GET /api/v1/plans/activity-types`，2025-10-02 复核后确认可直接联调；示例 `curl` 详见《[计划时间线事件字典说明](../docs/backend-requests/plan-timeline-activities.md#示例请求)》） | 🧪 联调中（时间线字典联动真实接口与详情缓存策略） | 默认命中生产接口 `/api/v1/plans/activity-types` 并记录协商缓存头；`mockPlanActivityTypes.json` 仅保留离线/自动化兜底并与线上契约对齐，Node Test 校验字段漂移；联调阶段同步验证时间线视图在接口降级或 `304` 命中时的回退体验 | 正式联调需补齐时间线事件与节点/提醒视图的字段对齐、缓存命中提示及多语言回退策略记录 |
| F-003 | 提醒策略配置 | `GET /api/v1/plans/{id}/reminders`、`PUT /api/v1/plans/{id}/reminders`、`GET /api/v1/plans/reminder-options` | 提醒渠道、触发时机、模板 ID 等字段 | ✅ 完成 | 🟡 规划中 | 设计默认策略样例及更新成功响应 | 后端提供提醒配置字典，详见《docs/backend-requests/plan-reminder-options.md》 |
| F-004 | 计划统计驾驶舱 | `GET /api/v1/plans/analytics` | 按状态、负责人、逾期风险等聚合数据 | 🧪 联调中 | 🛠️ 开发中 | `createMockPlanAnalyticsOverview` 生成驾驶舱样例，`usePlanAnalyticsController` 默认回退 Mock，提供手动切换实时/Mock 的控制；Node Test 覆盖查询参数拼装与空载荷错误回退 | 新增负责人负载与风险计划字段，详见《docs/backend-requests/plan-analytics-dashboard.md》；接口支持 `ownerId` 查询参数便于聚焦单个负责人；`PlanAnalyticsDashboard` 使用 Mock 渲染状态分布、负责人负载与风险卡片，等待后端联调确认数据映射 |
| F-005 | 节点执行与提醒控制 | `POST /api/v1/plans/{id}/nodes/{nodeId}/{action}`、`PUT /api/v1/plans/{id}/reminders/{reminderId}` | 需返回最新 `PlanDetailPayload`（节点、时间线、提醒） | ✅ 完成 | 🛠️ 开发中 | Mock 将在下个迭代替换为真实接口联调 | 详见《docs/backend-requests/plan-node-operations.md》，后端已交付节点开始/完成/交接与提醒规则更新接口，并新增 `actionType`/`completionThreshold` 字段及阈值自动跳过逻辑 |
| F-006 | 计划多视图驾驶舱 | `GET /api/v1/plans/board` 提供客户信息、计划时间窗，并自动裁剪重复/空的 `customerId`、`status` 参数 | 需要返回客户标识/名称及计划窗口（开始/结束）字段，新增 `atRiskPlans` 汇总暴露逾期+即将到期数量，并暴露 `referenceTime` 便于倒计时渲染 | ✅ 完成（基于 `PlanSearchCriteria` 聚合客户&时间桶并记录审计快照，补充 SQL 聚合 `at_risk_plans` 字段确保风险统计一致） | ✅ 完成（迭代 #2 视图扩展） | 扩展 `listMockPlans` 与 `planDetail` 样例补充客户字段，封装 `PlanByCustomerView`/`PlanCalendarView` 组件消费聚合结果，派生客户分组与日历事件并以 Node Test 校验排序、时间桶起止与时长计算 | 《docs/backend-requests/plan-board-view.md》已更新示例、参考时间字段、风险汇总 `atRiskPlans` 与审计说明；后端输出客户/时间桶排序一致（计划卡片按开始时间升序，遇到相同开始时间按计划 ID 升序）且多租户筛选与派生指标与真实接口保持一致，并新增持久化层集成测试覆盖多客户过滤；新增单元测试验证空白租户 ID 的全局视图访问与多客户去重；当无计划命中或服务层暂未回传聚合指标时 DTO 回退零指标对象，前端无需特殊判空；空租户 ID 的驾驶舱请求会以 `GLOBAL` 审计作用域记录；补充测试覆盖缺失 `plannedStartTime` 的计划仅在客户分组展示、不生成时间桶，前端同步在本地 Mock 中反映该场景；新增 `PlanBoardResponseTest` 确认空视图回退零指标并保持嵌套字段一致；新增 `PlanControllerTest.boardShouldOrderCustomerGroupsByTotals` 覆盖客户分组排序与时间桶顺序 |

> 当条目状态发生变化时，请同步更新根目录 README 的「前端阶段四迭代进度」摘要。
