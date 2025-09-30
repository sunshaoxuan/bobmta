# 部署资源概览

该目录与设计说明书中的 Docker Compose 拓扑保持一致，提供以下示例文件：

- `nginx/default.conf`：前端静态站点容器使用的 Nginx 配置，负责托管 `frontend/dist` 并将 `/api/` 请求代理到后端。
- `nginx/reverse-proxy.conf`：可选的边缘 Nginx 反向代理，统一 80/443 端口并加载 `deploy/certs` 中的 TLS 证书。
- `backend/Dockerfile` 与 `backend/backend.env`：构建后端镜像及其默认环境变量。
- `minio/README.md`：MinIO 初始化及数据卷说明。
- `certs/README.md`：证书挂载说明，指引在生产环境放置 `fullchain.pem`/`privkey.pem`。

Compose 运行时会将数据库与对象存储的数据挂载到 `deploy/postgres/data`、`deploy/minio/data`，请根据安全策略调整挂载路径并妥善备份。
