# 计划节点执行与提醒操作接口需求说明

## 背景

前端在迭代 #2 中交付了节点执行操作面板与提醒配置面板的交互占位，页面能够列出当前可执行的节点并展示提醒策略摘要。为继续推进业务画面，需要后端提供节点执行与提醒开关的真实接口，以便替换目前的本地状态模拟并完成端到端验证。

## 功能场景

1. **节点启动/完成**：在详情页中对处于 `PENDING` 或 `IN_PROGRESS` 状态的节点触发“开始执行”或“完成执行”操作，并返回更新后的节点树、时间线与提醒摘要。
2. **节点交接**：当节点拥有执行人时允许发起交接操作，指定新的负责人并写入备注，响应需回传最新的节点责任人信息。
3. **提醒启停与编辑占位**：点击提醒卡片上的“编辑”“启用/停用”时，应调用后端接口修改提醒策略的状态或内容，并返回更新后的提醒列表供页面同步缓存。

## 接口契约草案

| 操作 | HTTP 方法 | 路径 | 请求体 | 响应 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 启动节点 | POST | `/api/v1/plans/{planId}/nodes/{nodeId}/start` | `{ "operatorId": string }` | `PlanDetailPayload` | 触发成功后需更新时间线与节点状态 |
| 完成节点 | POST | `/api/v1/plans/{planId}/nodes/{nodeId}/complete` | `{ "operatorId": string, "resultSummary"?: string }` | `PlanDetailPayload` | resultSummary 为空时按后端默认处理 |
| 节点交接 | POST | `/api/v1/plans/{planId}/nodes/{nodeId}/handover` | `{ "operatorId": string, "assigneeId": string, "comment"?: string }` | `PlanDetailPayload` | 需校验节点允许交接的状态 |
| 更新提醒 | PUT | `/api/v1/plans/{planId}/reminders/{reminderId}` | `{ "active": boolean, "offsetMinutes"?: number }` | `PlanDetailPayload` | 前端当前仅需要启停能力，后续可拓展字段 |

返回结构建议复用 `PlanDetailPayload`，以便前端用同一缓存写入逻辑覆盖节点、时间线与提醒最新值。

## 数据与权限要求

- 所有接口需要使用现有的 Token 鉴权，并校验操作者具备计划执行或管理员权限。
- 节点操作需校验计划状态（例如仅允许已发布计划执行），失败时返回标准错误码及多语言提示键值。
- 提醒更新需保留操作审计，写入时间线事件供前端展示。

## Mock 策略

在后端交付前，前端继续使用 `frontend/src/mocks/planDetail.ts` 中的节点与提醒示例，通过本地状态模拟按钮反馈，并在 `frontend/tests/planNodes.test.mjs` 中校验节点筛选逻辑。接口联调完成后将替换为真实调用并扩展单测覆盖接口错误分支。

## 交付状态（2024-04-27）

- ✅ 后端已实现上述四个接口，统一返回 `PlanDetailPayload`，在响应中附带节点执行状态、时间线及提醒策略快照。
- ✅ 节点开始/完成接口支持请求体携带 `operatorId`，并会在审计日志内记录操作前后的计划详情。
- ✅ 新增节点交接与提醒规则更新的审计与时间线事件，满足前端对责任人变更与提醒启停的提示需求。
