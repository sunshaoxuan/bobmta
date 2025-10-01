# 计划节点执行与提醒操作接口需求说明

## 背景

前端在迭代 #2 中交付了节点执行操作面板与提醒配置面板的交互占位，页面能够列出当前可执行的节点并展示提醒策略摘要。为继续推进业务画面，需要后端提供节点执行与提醒开关的真实接口，以便替换目前的本地状态模拟并完成端到端验证。

## 功能场景

1. **节点启动/完成**：在详情页中对处于 `PENDING` 或 `IN_PROGRESS` 状态的节点触发“开始执行”或“完成执行”操作，并返回更新后的节点树、时间线与提醒摘要。
2. **节点交接**：当节点拥有执行人时允许发起交接操作，指定新的负责人并写入备注，响应需回传最新的节点责任人信息。
3. **提醒启停与编辑占位**：点击提醒卡片上的“编辑”“启用/停用”时，应调用后端接口修改提醒策略的状态或内容，并返回更新后的提醒列表供页面同步缓存。
4. **节点执行动作与阈值**：节点定义现在携带 `actionType`（枚举：`NONE`/`REMOTE`/`EMAIL`/`IM`/`LINK`/`FILE`）以及 `completionThreshold`（0～100），后端会在子节点达到阈值时自动补齐父节点完成并跳过剩余可选节点。

### 节点动作自动化

- 当节点处于 `EMAIL`、`IM`、`LINK` 或 `REMOTE` 类型时，`PlanService` 会在 `startNode`/`completeNode` 流程中调用模板服务渲染上下文（计划ID、节点名称、操作者等），随后通过通知网关派发邮件或即时消息，或生成用于远程/链接类动作的目标地址。
- 每次自动化执行都会记录到新的 `PlanActionHistory` 仓储中，字段包括动作类型、模板引用、执行状态与错误信息。
- 时间线新增 `NODE_ACTION_EXECUTED` 事件，前端可依据 `actionStatus`、`actionMessage`、`actionError` 等属性展示成功/失败详情。
- 当通知网关返回失败或模板渲染异常时，计划状态更新仍会提交，但历史记录与时间线会以失败状态展示，便于前端提示并允许用户重试。

## 接口契约草案

| 操作 | HTTP 方法 | 路径 | 请求体 | 响应 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 启动节点 | POST | `/api/v1/plans/{planId}/nodes/{nodeId}/start` | `{ "operatorId": string }` | `PlanDetailPayload` | 触发成功后需更新时间线与节点状态 |
| 完成节点 | POST | `/api/v1/plans/{planId}/nodes/{nodeId}/complete` | `{ "operatorId": string, "resultSummary"?: string }` | `PlanDetailPayload` | 当子节点达到父节点 `completionThreshold` 时，响应会携带父节点的自动完成与被跳过的兄弟节点 |
| 节点交接 | POST | `/api/v1/plans/{planId}/nodes/{nodeId}/handover` | `{ "operatorId": string, "assigneeId": string, "comment"?: string }` | `PlanDetailPayload` | 需校验节点允许交接的状态 |
| 更新提醒 | PUT | `/api/v1/plans/{planId}/reminders/{reminderId}` | `{ "active": boolean, "offsetMinutes"?: number }` | `PlanDetailPayload` | 前端当前仅需要启停能力，后续可拓展字段 |

返回结构建议复用 `PlanDetailPayload`，以便前端用同一缓存写入逻辑覆盖节点、时间线与提醒最新值。

## 数据与权限要求

- 所有接口需要使用现有的 Token 鉴权，并校验操作者具备计划执行或管理员权限。
- 节点操作需校验计划状态（例如仅允许已发布计划执行），失败时返回标准错误码及多语言提示键值。
- 提醒更新需保留操作审计，写入时间线事件供前端展示。
- 自动阈值处理会在时间线上写入 `plan.activity.nodeAutoCompleted` 与 `plan.activity.nodeSkipped` 事件，前端可据此提示“系统自动完成/跳过”原因。

## Mock 策略

在后端交付前，前端继续使用 `frontend/src/mocks/planDetail.ts` 中的节点与提醒示例，通过本地状态模拟按钮反馈，并在 `frontend/tests/planNodes.test.mjs` 中校验节点筛选逻辑。接口联调完成后将替换为真实调用并扩展单测覆盖接口错误分支。

## 状态更新

- ✅ 后端已交付节点执行与提醒更新接口，前端计划详情面板现已切换为真实调用并保留缓存回退逻辑，后续联调将重点验证错误码与权限提示。
- 🔄 前端已完成权限/错误码联调，并补充失败态重试按钮、参数回填入口及时间线类别筛选提示；下一阶段将聚焦计划详情路由与列表筛选重构的协同规划。
