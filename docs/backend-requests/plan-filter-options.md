# 计划列表筛选字典接口说明

## 背景

前端计划列表在迭代 #2 中已经提供负责人、状态、客户及时间窗口的筛选控件，但早期仍依赖静态枚举与硬编码的时间范围。为推进 F-001 的联调，后端已输出一组随租户变化的筛选元数据，包含可选状态、计划负责人、关联客户以及建议的预计时间窗，便于前端动态渲染控件与提示文案，并在 2025-09-29 发布至生产环境。

## 功能场景

1. **筛选控件初始化**：前端在进入计划列表页面时调用后端接口，获取状态/负责人/客户选项及默认时间窗提示，生成多语言化的筛选组件。
2. **租户隔离**：同一接口支持传入 `tenantId`，仅返回对应租户下的负责人与客户，避免跨租户数据泄露。
3. **统计提示**：每个选项附带当前计划数量，前端可据此在下拉菜单中提示剩余计划规模。

## 接口契约

| 操作 | HTTP 方法 | 路径 | 查询参数 | 响应 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 获取筛选字典 | GET | `/api/v1/plans/filter-options` | `tenantId?` | `PlanFilterOptionsResponse` | 需要管理员或运维角色 |

### 示例请求

```bash
curl \
  -H "Accept-Language: zh-CN" \
  "${HOST}/api/v1/plans/filter-options?tenantId=acme"
```

该接口已经在生产环境启用缓存与租户隔离策略，可直接用于联调与回归测试；当后端返回 `304 Not Modified` 时，前端仍需按照缓存刷新策略主动降级至本地兜底数据。可使用以下命令快速校验契约：

```bash
curl \
  -H "Accept-Language: zh-CN" \
  "${HOST}/api/v1/plans/filter-options?tenantId=acme"
```

### 响应结构

```json
{
  "statusLabel": "计划状态",
  "statuses": [
    { "value": "DESIGN", "label": "设计中", "count": 3 },
    { "value": "SCHEDULED", "label": "已排期", "count": 1 }
  ],
  "ownerLabel": "负责人",
  "owners": [
    { "value": "admin", "label": "admin", "count": 2 }
  ],
  "customerLabel": "客户",
  "customers": [
    { "value": "cust-001", "label": "cust-001", "count": 2 }
  ],
  "plannedWindow": {
    "label": "计划时间窗",
    "hint": "可按计划的预计开始与结束时间筛选",
    "start": "2024-05-01T09:00:00+09:00",
    "end": "2024-05-12T18:00:00+09:00"
  }
}
```

- `statuses` 永远按 `DESIGN → SCHEDULED → IN_PROGRESS → COMPLETED → CANCELED` 顺序返回，即使某状态当前数量为 0。
- `owners`、`customers` 去重后按计划数量倒序排列，相同数量再按字典序排序。
- `plannedWindow` 在缺少有效时间信息时返回 `null`。

## 验收要点

- 携带 `tenantId` 参数时仅返回对应租户的数据，数量统计同步受限。
- 多语言由后端依据请求语言在服务端完成，前端无需额外翻译。
- 返回的数量字段可直接用于前端标注或排序，不需要再额外查询。

## 联调与 Mock 指南

- **默认调用生产接口**：前端在联调及回归时应直接命中生产环境的 `GET /api/v1/plans/filter-options`。如需快速验证，可使用前文示例中的 `curl -H "Accept-Language: zh-CN" "${HOST}/api/v1/plans/filter-options?tenantId=acme"` 命令校验响应字段与多语言标签。
- **Mock 仅作为兜底**：`frontend/src/mocks/mockPlanFilterOptions.json` 与相关查询方法（如 `queryMockPlanSummaries`）仅在离线或接口异常时启用，需保持与线上契约一致并通过 Node Test 持续校验；当真实接口返回 `304 Not Modified` 时，前端应按缓存策略回退本地缓存并记录刷新时间。
- **联调记录**：2025-10-02 与后端确认接口持续可用后，联调结果与缓存降级策略需同步更新至《frontend/FRONTEND_REQUIREMENTS.md》及根目录 README，以保持团队协同。

## 交付状态

- ✅ 后端已于 2025-09-29 在生产环境上线 `GET /api/v1/plans/filter-options`，并在控制器单测覆盖多语言标签、数量与时间窗字段；接口契约与示例如上所示，可直接用于线上联调。2025-10-02 再次与后端确认接口保持可用，`curl` 示例同上。
- 🔄 前端待接入该接口以替换现有的静态筛选枚举，并补充离线缓存策略与 `304` 回退处理。
