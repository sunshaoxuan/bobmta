# BOB MTA Maintain Assistants 平台 - 阶段二后端框架

该仓库用于逐步实现《BOB MTA（Maintain Assistants）综合运维平台 详细设计说明书》中的平台。本次迭代聚焦第二阶段目标：建立后端核心框架与领域模块雏形，提供统一响应格式、认证鉴权骨架以及符合详细设计的关键 API 草稿数据。

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
| `GET /api/v1/customers` | 客户列表（分页 + 关键字/地区过滤） | 返回示例数据，结构贴合详细设计中的字段规划 |
| `GET /api/v1/customers/{id}` | 客户详情视图 | 演示自定义字段分组、标签等信息 |
| `GET /api/v1/plans` | 运维计划列表 | 支持按客户、状态筛选，返回计划进度摘要 |
| `GET /api/v1/plans/{id}` | 运维计划详情 | 含节点执行模式需要的结构化数据 |
| `GET /api/ping` | 健康检查 | 便于部署级联路由验证 |

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
