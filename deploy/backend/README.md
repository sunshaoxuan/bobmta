# 后端容器配置

本目录包含：

- `Dockerfile`：基于 Maven 多阶段构建 `backend` 模块并产出可运行的 Spring Boot 镜像。
- `backend.env`：示例环境变量文件，供 `docker-compose.yml` 读取数据库、JWT 和 MinIO 的默认配置。
- `../../docs/database-migrations.md`：数据库迁移与初始化说明，包含 Flyway 自动执行策略与手动命令。

根据部署环境需求，可复制 `backend.env` 为本地 `.env` 并替换敏感信息。Compose 会将变量注入后端容器，同时复用数据库和 MinIO 容器的同名变量，保持凭证一致。

当容器启动时，后端会根据 `application.yml` 中的 Flyway 配置自动执行 `db/migration` 下的脚本。如需在宿主机手动迁移，可进入源码目录执行 `./mvnw -pl backend flyway:migrate`，详情参见上文迁移文档。
