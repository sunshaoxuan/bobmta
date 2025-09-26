# BOB MTA Maintain Assistants 平台 - 阶段二后端框架

本仓库根据《BOB MTA（Maintain Assistants）综合运维平台 详细设计说明书》分阶段实现平台。本次提交完成第二阶段目标：

- 完成后端基础框架，提供统一响应、异常拦截、JWT 鉴权与 RBAC 校验；
- 搭建内存版“用户、客户、计划”领域能力，覆盖账号激活、角色分配、客户视图与计划流程等关键场景；
- 编写针对服务、控制器与安全组件的单元测试，Jacoco 预设覆盖率目标大于 80%，为后续持久化改造提供安全网。

## 项目结构

```
backend/   # Spring Boot 3 后端服务，暴露阶段二所需的 REST API
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
| `GET /api/v1/plans` | 运维计划列表 | 支持按客户、状态过滤并返回进度摘要 |
| `GET /api/v1/plans/{id}` | 运维计划详情 | 展示流程节点树形结构 |
| `GET /api/ping` | 健康检查 | 返回 `{status: ok}` |

### 测试与覆盖率

```bash
cd backend
mvn verify
```

该命令会运行全部单元测试并在 `backend/target/site/jacoco/index.html` 生成 Jacoco 覆盖率报表。若初次执行无法下载依赖，可根据环境配置 Maven 镜像。

## 下一步计划

- 将内存实现替换为基于 PostgreSQL + MyBatis 的持久化访问层，补充数据库建模脚本；
- 引入模板中心、文件服务、审计日志等模块骨架，补全跨模块协作接口；
- 与前端协同定义 OpenAPI 契约，扩展状态管理与国际化能力；
- 补充更多集成测试并接入 CI，持续维持覆盖率在 80% 以上。
