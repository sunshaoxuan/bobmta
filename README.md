# BOB MTA Maintain Assistants 平台 - 阶段二后端框架

该仓库用于逐步实现《BOB MTA（Maintain Assistants）综合运维平台 详细设计说明书》中的平台。本次迭代聚焦第二阶段目标：

- 巩固后端核心框架，补齐统一响应、异常与鉴权链路；
- 建立“用户与权限”领域模块的内存实现，覆盖账号创建、激活、角色分配全流程；
- 编写端到端与领域单元测试，使后端整体覆盖率保持在 80% 以上，为后续数据库落地提供安全网。

## 项目结构

```
backend/   # Spring Boot 3 后端服务，提供 REST API
frontend/  # React + Vite 前端单页应用
```

## 快速开始

### 后端

```bash
cd backend
mvn spring-boot:run
```

服务启动后可访问下列示例接口：

| 接口 | 描述 | 备注 |
| --- | --- | --- |
| `POST /api/v1/auth/login` | 账号登录（内存账户） | 账号：`admin` / `admin123`、`operator` / `operator123` |
| `GET /api/v1/auth/me` | 获取当前登录用户信息 | 需在 `Authorization: Bearer <token>` 头中携带登录返回的 token |
| `POST /api/v1/users` | 创建系统用户并发放激活链接 | 需管理员角色，返回激活 token 与初始状态 |
| `POST /api/v1/users/activation` | 校验激活 token 并启用账号 | 激活接口对未登录用户开放以便邮件链接调用 |
| `POST /api/v1/users/{id}/activation/resend` | 重新发放激活链接 | 需管理员角色，用于重发邮件 |
| `POST /api/v1/users/{id}/roles` | 更新用户角色集合 | 角色自动转换为大写并覆盖旧配置 |
| `GET /api/v1/customers` | 客户列表（分页 + 关键字/地区过滤） | 返回示例数据，结构贴合详细设计中的字段规划 |
| `GET /api/v1/customers/{id}` | 客户详情视图 | 演示自定义字段分组、标签等信息 |
| `GET /api/v1/plans` | 运维计划列表 | 支持按客户、状态筛选，返回计划进度摘要 |
| `GET /api/v1/plans/{id}` | 运维计划详情 | 含节点执行模式需要的结构化数据 |
| `GET /api/ping` | 健康检查 | 便于部署级联路由验证 |

#### 测试与覆盖率

```bash
cd backend
mvn verify
```

命令会执行全部单元测试并生成 JaCoCo 覆盖率报表（`backend/target/site/jacoco/index.html`），覆盖率目标保持在 80% 以上，可作为后续持续集成基线。本阶段新增的用户模块同样包含服务与控制层测试，覆盖激活超时、重复用户名、角色分配等关键路径。

> 说明：首次执行需要能够访问 Maven 中央仓库以下载依赖，若运行环境受限请配置镜像或私有仓库。

### 前端

```bash
cd frontend
npm install
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`，页面会自动请求后端 `GET /api/ping` 接口并展示结果。

> 说明：由于运行环境可能无法直接访问 npm 官方源，如遇安装失败，可配置镜像源（例如 `npm config set registry https://registry.npmmirror.com`）。

## 下一步计划

- 将当前内存实现替换为基于 PostgreSQL + MyBatis 的持久化访问层，并补充数据库建模脚本。
- 引入多租户、模板中心、文件服务、审计日志等模块的领域骨架，实现跨模块协作接口。
- 与前端协同定义 OpenAPI/接口契约，扩展前端状态管理以对接新增 API。
- 补充单元测试与集成测试，构建覆盖率>80%的质量保障体系，并接入 CI。
