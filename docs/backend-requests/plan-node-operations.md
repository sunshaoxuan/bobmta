# 计划节点执行与提醒操作接口需求说明

## 背景

前端在迭代 #2 中交付了节点执行操作面板与提醒配置面板的交互占位，页面能够列出当前可执行的节点并展示提醒策略摘要。为继续推进业务画面，需要后端提供节点执行与提醒开关的真实接口，以便替换目前的本地状态模拟并完成端到端验证。

## 功能场景

1. **节点启动/完成**：在详情页中对处于 `PENDING` 或 `IN_PROGRESS` 状态的节点触发“开始执行”或“完成执行”操作，并返回更新后的节点树、时间线与提醒摘要。
2. **节点交接**：当节点拥有执行人时允许发起交接操作，指定新的负责人并写入备注，响应需回传最新的节点责任人信息。
3. **提醒启停与编辑占位**：点击提醒卡片上的“编辑”“启用/停用”时，应调用后端接口修改提醒策略的状态或内容，并返回更新后的提醒列表供页面同步缓存。
4. **节点执行动作与阈值**：节点定义现在携带 `actionType`（枚举：`NONE`/`REMOTE`/`EMAIL`/`IM`/`LINK`/`FILE`）以及 `completionThreshold`（0～100），后端会在子节点达到阈值时自动补齐父节点完成并跳过剩余可选节点。

### 节点动作自动化

- 当节点处于 `EMAIL`、`IM`、`LINK`、`REMOTE` 或 `API_CALL` 类型时，`PlanService` 会在 `startNode`/`completeNode`/`handoverNode` 流程中调用模板服务渲染上下文（计划ID、计划状态、节点名称、节点责任人、操作者、触发来源、执行结果等），随后通过通知网关派发邮件或即时消息，生成用于远程/链接类动作的目标地址，或向外部工作流/自动化平台发起 API 调用。
- 每次自动化执行都会记录到新的 `PlanActionHistory` 仓储中，字段包括动作类型、模板引用、执行状态、上下文（计划/节点/操作者等）与错误信息，便于审计。`metadata` 字段会落盘模板与网关返回的详情，如 `attempts`（重试次数）、`endpoint`（链接或远程会话地址）、`artifactName`（远程脚本/附件名）、`provider`（邮件/IM 服务商）等，供运营追溯。
- API 调用场景会将模板 `metadata` 中非敏感键（如 `method`、`scenario` 等）与通知网关返回值写入历史，并过滤掉以 `header.` 开头的敏感首部内容，最终在时间线中以 `meta.method`、`meta.endpoint`、`meta.status` 等字段供前端展示；若模板缺失 `endpoint` 则直接以失败写入历史并提示 `plan.error.nodeActionApiEndpointMissing`。
- 时间线新增 `NODE_ACTION_EXECUTED` 事件，前端可依据 `actionStatus`、`actionMessage`、`actionError`、`meta.*`（模板/网关元数据、尝试次数、远程会话地址、生成的知识库链接等）与 `context.*`（计划、节点、责任人、触发来源、执行结果等）属性展示成功/失败详情并支持筛选；事件中新增的 `actionId` 字段与 `PlanActionHistory` 主键对齐，便于点击时间线直接跳转或请求完整历史记录。缺失链接的场景会返回 `actionStatus = SKIPPED` 并附带默认错误文案 `plan.error.nodeActionLinkMissing`，便于提示人工补录。
- 当通知网关返回失败、无响应或模板渲染异常时，服务会按通道自动重试至多 3 次，并在 `metadata.attempts` 中记录尝试次数。若仍失败则以失败状态写入历史与时间线，同时通过 `metadata.reason` 标记具体原因（如 `EXCEPTION`、`NO_RESPONSE`），计划状态更新仍会提交，便于前端提示并允许用户发起人工重试。
- 当节点动作类型为 `FILE` 时，系统不会触发任何网关或模板渲染，执行会以 `actionStatus = SKIPPED` 写入动作历史与时间线，并在 `metadata.reason`
  字段标记 `NOT_SUPPORTED`，提醒操作者改为人工处理。

## 接口契约草案

| 操作 | HTTP 方法 | 路径 | 请求体 | 响应 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 启动节点 | POST | `/api/v1/plans/{planId}/nodes/{nodeId}/start` | `{ "operatorId": string }` | `PlanDetailPayload` | 触发成功后需更新时间线与节点状态 |
| 完成节点 | POST | `/api/v1/plans/{planId}/nodes/{nodeId}/complete` | `{ "operatorId": string, "resultSummary"?: string }` | `PlanDetailPayload` | 当子节点达到父节点 `completionThreshold` 时，响应会携带父节点的自动完成与被跳过的兄弟节点 |
| 节点交接 | POST | `/api/v1/plans/{planId}/nodes/{nodeId}/handover` | `{ "operatorId": string, "assigneeId": string, "comment"?: string }` | `PlanDetailPayload` | 需校验节点允许交接的状态 |
| 更新提醒 | PUT | `/api/v1/plans/{planId}/reminders/{reminderId}` | `{ "active": boolean, "offsetMinutes"?: number }` | `PlanDetailPayload` | 前端当前仅需要启停能力，后续可拓展字段 |
| 获取动作历史 | GET | `/api/v1/plans/{planId}/actions` | 无 | `PlanActionHistory[]` | 返回最近的动作执行历史，列表按触发时间升序，元素包含 `context.*` 与 `meta.*` 详情，便于前端渲染时间线或溯源 |

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
