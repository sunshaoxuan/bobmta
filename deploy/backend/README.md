# 后端容器配置

本目录包含：

- `Dockerfile`：基于 Maven 多阶段构建 `backend` 模块并产出可运行的 Spring Boot 镜像。
- `backend.env`：示例环境变量文件，供 `docker-compose.yml` 读取数据库、JWT 和 MinIO 的默认配置。

根据部署环境需求，可复制 `backend.env` 为本地 `.env` 并替换敏感信息。Compose 会将变量注入后端容器，同时复用数据库和 MinIO 容器的同名变量，保持凭证一致。
