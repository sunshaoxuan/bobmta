# 计划时间线事件字典说明

## 背景

前端在 F-002「计划详情与时间线视图」中需要基于后端返回的时间线事件类型展示图标、文案及属性细节。此前仅有事件 `type` 与 `message` 字段，无法直接得知不同事件包含的属性语义及可用的多语言文案。为支撑时间线视图的信息展示与过滤需求，后端补充时间线事件字典接口以及对应的属性描述。

## 新增接口

| HTTP 方法 | 路径 | 描述 |
| --- | --- | --- |
| GET | `/api/v1/plans/activity-types` | 返回全部 `PlanActivityType` 的元数据，包括可用的消息键列表以及每个属性的含义描述 |

### 响应结构

```json
[
  {
    "type": "PLAN_CREATED",
    "messages": [
      { "key": "plan.activity.created", "message": "计划创建" }
    ],
    "attributes": [
      { "name": "title", "descriptionKey": "plan.activity.attr.title", "description": "事件发生时的计划标题快照" },
      { "name": "owner", "descriptionKey": "plan.activity.attr.owner", "description": "事件发生时的计划负责人" }
    ]
  }
]
```

> `message`、`description` 字段会根据 `Accept-Language` 自动返回对应语言文本，`key` 与 `descriptionKey` 可供前端在本地多语言资源中做二次渲染或作为图标映射依据。

## 事件与属性对照

| 类型 | 消息键 | 主要属性 |
| --- | --- | --- |
| `PLAN_CREATED` | `plan.activity.created` | `title`（计划标题快照）、`owner`（当时负责人） |
| `PLAN_UPDATED` | `plan.activity.definitionUpdated` | `title`、`timezone`、`participantCount` |
| `PLAN_PUBLISHED` | `plan.activity.published` | `status`、`operator` |
| `PLAN_CANCELLED` | `plan.activity.cancelled` | `reason`、`operator` |
| `PLAN_COMPLETED` | `plan.activity.completed` | `operator` |
| `PLAN_HANDOVER` | `plan.activity.handover` | `oldOwner`、`newOwner`、`operator`、`participantCount`、`note` |
| `NODE_STARTED` | `plan.activity.nodeStarted` | `nodeName`、`assignee`、`operator` |
| `NODE_COMPLETED` | `plan.activity.nodeCompleted` | `nodeName`、`operator`、`result` |
| `NODE_ACTION_EXECUTED` | `plan.activity.nodeActionExecuted` | `nodeName`、`actionType`、`actionStatus`、`actionMessage`、`actionTrigger`、`actionError` |
| `NODE_HANDOVER` | `plan.activity.nodeHandover` | `nodeName`、`previousAssignee`、`newAssignee`、`operator`、`comment` |
| `NODE_AUTO_COMPLETED` | `plan.activity.nodeAutoCompleted` | `nodeName`、`threshold`、`completedChildren`、`totalChildren` |
| `NODE_SKIPPED` | `plan.activity.nodeSkipped` | `nodeName`、`parentNodeId`、`parentNode` |
| `REMINDER_POLICY_UPDATED` | `plan.activity.reminderUpdated`、`plan.activity.reminderRuleUpdated` | `ruleCount`、`offsetMinutes`、`active` |

## 验收要点

1. 前端请求 `/api/v1/plans/activity-types` 可获得上述类型与属性的完整列表，并在不同语言下返回对应翻译。
2. 时间线接口（`GET /api/v1/plans/{id}/timeline`）继续返回 `PlanActivityResponse`，其中 `type` 字段与字典接口中的 `type` 对应。
3. 属性描述的多语言文案在 `messages.properties`、`messages_zh.properties`、`messages_ja.properties` 中维护，前端可直接使用描述或根据 `descriptionKey` 做自定义渲染。

## 更新记录

- 2025-09-29：新增时间线事件字典接口与属性描述，满足 F-002 的时间线展示需求。

## 交付状态

- ✅ 后端已实现 `/api/v1/plans/activity-types` 接口并补充事件属性描述，多语言文案已同步落库。
- 🔄 前端待基于字典接口完成时间线图标与描述渲染的联调与缓存策略。
