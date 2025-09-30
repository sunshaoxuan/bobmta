# PostgreSQL 数据卷

`docker-compose.yml` 会把数据库数据目录挂载到 `deploy/postgres/data`。若需要持久化或备份，可将该目录映射到宿主机其他位置，并确保权限允许 `postgres` 用户写入。

生产环境请修改 `POSTGRES_PASSWORD`，并根据需要在 `backend/backend.env` 或单独的 `.env` 文件中覆盖数据库名称与账号。
