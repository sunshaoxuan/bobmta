# 数据库迁移与初始化

本项目后端使用 [Flyway](https://flywaydb.org/) 管理 PostgreSQL 的结构与数据迁移，相关配置和脚本均存放在 `backend/src/main/resources/db/` 目录。

## 目录结构

- `db/schema.sql`：面向开发或测试环境的基线结构脚本，覆盖计划、节点、提醒、活动等所有表与索引定义，并附带迁移策略说明。对于需要手动建库的场景，可直接执行此文件完成初始化。
- `db/data.sql`：示例数据，帮助快速体验计划聚合与提醒功能。执行顺序建议在 `schema.sql` 之后。
- `db/migration/V__*.sql`：Flyway 版本化迁移脚本。每次结构或索引调整必须新增一个新的版本文件，而不是修改历史脚本。

## 应用自动执行迁移

Spring Boot 配置已开启 Flyway：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

因此在应用启动时会自动扫描 `db/migration` 下的脚本并执行增量迁移。若数据库为空，将自动创建基线表结构。

## 手动执行迁移

对于容器外的调试或 CI 场景，可使用 Maven 插件显式运行迁移：

```bash
./mvnw -pl backend flyway:migrate
```

若需要重置数据库并重新应用全部脚本（仅限测试环境），可运行：

```bash
./mvnw -pl backend flyway:clean flyway:migrate
```

> ⚠️ `flyway:clean` 会删除指定数据库中的所有对象，请谨慎在生产环境使用。

## 迁移策略要点

1. 任何结构变更都应通过新增 `V{n}__description.sql` 文件完成，命名应清晰描述变更内容。
2. 已发布的脚本禁止改写，若需要修复历史问题，请通过新的版本脚本完成。
3. 对于复杂数据迁移，建议编写可重复执行的 SQL（如使用 `CREATE TABLE IF NOT EXISTS` 或 `ON CONFLICT DO NOTHING`）。
4. 如果需要快速初始化或排查问题，可参考 `schema.sql` 与 `data.sql` 手动创建或回填数据。
