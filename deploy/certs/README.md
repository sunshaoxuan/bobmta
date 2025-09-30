# TLS 证书占位

将生产环境的证书文件放在此目录中并保持以下文件名，以便 docker-compose 中的 `nginx` 代理容器能够加载：

- `fullchain.pem`：包含服务器证书和中间证书链。
- `privkey.pem`：服务器私钥。

建议通过 `.gitignore` 忽略真实证书文件，仅在部署环境中挂载。开发或测试阶段若不启用 TLS，可保持该目录为空，并在运行 Compose 时跳过 `proxy` profile。
