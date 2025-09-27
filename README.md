# BOB MTA Maintain Assistants 平台 - 阶段三后端功能集

本仓库根据《BOB MTA（Maintain Assistants）综合运维平台 详细设计说明书》分阶段实现平台。本次提交完成第三阶段目标：

- 在阶段二基础上扩充“标签、模板、自定义字段、文件与审计”五大模块，串联客户、计划等核心实体，形成完整后台功能链路；
- 将客户视图与标签/自定义字段服务打通，支持多维筛选与档案扩展；模板服务提供邮件、IM、链接、远程连接等多形态模板渲染，并支持为远程桌面模板生成 `.rdp` 附件与命令提示；
- 增强运维计划模块，新增计划创建/更新/删除/发布/取消接口与节点执行、文件挂载能力，并支持单计划导出及租户级订阅的 ICS 日历；
- 实现文件元数据登记与下载地址生成，并通过审计服务集中记录关键操作；
- 为新增模块补充服务与控制器单元测试，整体测试场景覆盖率保持在 80% 以上，为持久化与联调阶段夯实质量基线。

## 项目结构

```
backend/   # Spring Boot 3 后端服务，聚合阶段三功能所需的 REST API
frontend/  # React + Vite 前端占位，后续阶段将继续完善
```

## 后端快速开始

```bash
cd backend
mvn spring-boot:run
```

应用启动后可尝试以下示例接口：

| 接口 | 描述 | 备注 |
| --- | --- | --- |
| `POST /api/v1/auth/login` | 账号登录（内存账户） | 预置账号：`admin`/`admin123`、`operator`/`operator123` |
| `GET /api/v1/auth/me` | 获取当前登录用户信息 | 需要在 `Authorization: Bearer <token>` 中携带登录返回的 Token |
| `POST /api/v1/users` | 创建系统用户并发放激活链接 | 需管理员角色 |
| `POST /api/v1/users/activation` | 校验激活 Token 并启用账号 | 激活接口对未登录用户开放 |
| `POST /api/v1/users/{id}/activation/resend` | 重新发放激活链接 | 需管理员角色 |
| `PUT /api/v1/users/{id}/roles` | 更新用户角色集合 | 角色名自动标准化为 `ROLE_*` |
| `GET /api/v1/customers` | 客户列表 | 支持按地区与关键字过滤（内存数据） |
| `GET /api/v1/customers/{id}` | 客户详情 | 展示联系人、自定义字段等结构 |
| `GET /api/v1/custom-fields` | 自定义字段定义列表 | 支持动态档案字段配置 |
| `PUT /api/v1/custom-fields/customers/{id}` | 更新客户自定义字段值 | 支持增改非结构化字段 |
| `GET /api/v1/plans` | 运维计划列表 | 支持按客户、状态、时间范围过滤并返回进度摘要 |
| `POST /api/v1/plans` | 创建运维计划 | 接收节点树结构与参与人列表，初始状态为 DESIGN |
| `PUT /api/v1/plans/{id}` | 更新运维计划 | 仅在 DESIGN 状态下允许修改时间、节点等信息 |
| `DELETE /api/v1/plans/{id}` | 删除运维计划 | DESIGN 状态下可删除，删除时写入审计日志 |
| `POST /api/v1/plans/{id}/publish` | 发布计划 | 根据开始时间切换为 SCHEDULED/IN_PROGRESS，并记录审计 |
| `POST /api/v1/plans/{id}/cancel` | 取消计划 | 将状态置为 CANCELED，支持附带取消原因 |
| `GET /api/v1/plans/{id}` | 运维计划详情 | 展示流程节点树及执行状态/附件列表 |
| `POST /api/v1/plans/{id}/nodes/{nodeId}/start` | 开始执行节点 | 节点状态切换为 IN_PROGRESS，并记录操作人 |
| `POST /api/v1/plans/{id}/nodes/{nodeId}/complete` | 完成节点 | 提交执行结果、日志与附件，自动推进计划进度 |
| `GET /api/v1/plans/{id}/ics` | 导出单计划 ICS | 生成 `text/calendar` 文件，可导入 Outlook/Google 日历 |
| `GET /api/v1/calendar/tenant/{tenant}.ics` | 租户计划订阅 | 输出租户可见计划的 ICS 订阅源 |
| `GET /api/v1/tags` | 标签管理 | 支持按作用域筛选、关联客户/计划 |
| `POST /api/v1/templates/{id}/render` | 模板渲染 | 根据上下文替换占位符，返回渲染结果 |
| `POST /api/v1/files` | 文件元数据登记 | 生成对象存储键及下载地址 |
| `GET /api/v1/audit-logs` | 审计日志查询 | 需管理员角色 |
| `GET /api/ping` | 健康检查 | 返回 `{status: ok}` |

### 测试与覆盖率

```bash
cd backend
mvn verify
```

该命令会运行全部单元测试并在 `backend/target/site/jacoco/index.html` 生成 Jacoco 覆盖率报表。若初次执行无法下载依赖，可根据环境配置 Maven 镜像。

## 下一步计划

- 引入 PostgreSQL + MyBatis 持久化层，实现标签、模板、文件、自定义字段等实体的数据库存储；
- 与前端协同定义 OpenAPI 契约，扩展状态管理与国际化能力；
- 补充更多集成测试并接入 CI，持续维持覆盖率在 80% 以上；
- 推进对象存储与日历订阅等外部集成，完善阶段四及之后的联调准备。
