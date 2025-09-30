# MinIO 配置说明

`docker-compose.yml` 默认以 `minio/minio` 镜像启动对象存储，并将数据目录挂载到 `deploy/minio/data`。首次启动后可通过浏览器访问 `http://localhost:9001`，使用 `.env` 中的 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` 登录控制台并创建业务桶。

示例中 `deploy/backend/backend.env` 预置桶名为 `plan-files`，可手动在控制台或使用 MinIO Client（`mc`）创建：

```bash
mc alias set bobmta http://localhost:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"
mc mb bobmta/plan-files
```

生产环境请修改凭证，并考虑关闭控制台端口或在外部反向代理中添加访问控制。
