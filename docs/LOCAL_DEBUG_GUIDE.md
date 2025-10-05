# 本地调试指南

> 本文档提供在 Windows 环境下拉起 BOB MTA 项目的后端与前端服务、执行自测与排障的步骤。若你采用的是其他操作系统，可据此调整命令格式。

## 1. 先决条件

| 组件 | 建议版本 | 说明 |
| --- | --- | --- |
| Git | 最新稳定版 | 便于获取代码与管理分支 |
| Java SDK | 17 LTS | Spring Boot 后端构建与运行所需 |
| Maven | 3.9+ | 如使用 mvnw 包装器，可省略本地安装；本文档沿用 mvn 命令示例 |
| Node.js | 18 LTS | 前端构建与单元测试所需 |
| TypeScript | 与 package.json 编译器匹配 (>=5.x) | 
pm run build 依赖 	sc 可执行文件 |
| Docker / Docker Compose (可选) | 24+ | 若需启动 PostgreSQL、MinIO、Nginx 等外部服务 |

> 若处于离线或受限网络环境，需提前下载 JDK、Maven、Node.js 安装包，并确认 
pm 离线缓存可用。

## 2. 仓库准备

`ash
# 获取最新代码
git clone https://github.com/sunshaoxuan/bobmta.git
cd bobmta
# 同步主干
git fetch origin
git checkout main
git pull --ff-only
`

如需调试 PR，请切换到对应分支或在本地合并主干后再进行调试。

## 3. 后端环境搭建

1. **配置环境变量**
   - 确保 JAVA_HOME 指向 JDK 17 安装目录，MAVEN_HOME（可选）指向 Maven 安装目录。
   - 将 %JAVA_HOME%\bin、%MAVEN_HOME%\bin 添加到 PATH。

2. **安装依赖**
   - 第一次准备后端依赖时，请在仓库根目录执行 mvn -f backend/pom.xml dependency:go-offline 以预下载 Maven 依赖和插件缓存。
   - 若命令仍提示 org.flywaydb:flyway-database-postgresql 相关错误，请直接从 ackend/pom.xml 删除该依赖条目；该模块属于 Flyway Teams 版本，社区版只需保留 lyway-core 和 PostgreSQL 驱动即可。
lyway-core 和 PostgreSQL 驱动即可。
