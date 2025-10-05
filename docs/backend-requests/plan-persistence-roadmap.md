# Plan Persistence Roadmap (Phase IV)

## Current State Snapshot
- Domain services仍主要依赖 InMemoryPlanService；持久化仓储（PlanAggregateMapper、PlanPersistenceAnalyticsRepository 等）仅在统计和查询层被使用。
- PostgreSQL schema 已通过 Flyway V1–V6 创建，Plan 模块的 CRUD / Reminder / Timeline 表结构具备生产化基础。
- Testcontainers 集成测试覆盖 PlanAggregateMapper 的筛选、节点、提醒、附件、活动等核心 SQL；但业务层尚未切换至仓储实现。

## Gap Assessment
1. **服务实现缺失**：需要实现 PersistencePlanService，以仓储为数据源，同时复用现有领域逻辑（发布、取消、节点执行、审计等）。
2. **事务边界梳理**：持久化实现需对复杂操作（发布、执行、提醒更新）定义一致的事务模板，取代内存态的原子操作。可复用 Spring @Transactional + MyBatis 映射。
3. **审计与通知集成**：InMemory 服务负责写入审计、触发通知，迁移后需保证 PersistencePlanService 能在持久化操作成功后调用相同的审计记录与通知适配器。
4. **缓存与并发**：目前的缓存策略由 InMemory 服务维护。迁移时需评估计划读写缓存是否继续存在或由数据库视图/Redis 替代。
5. **回归测试**：需要构建 API 层 / 服务层组合测试，验证持久化行为与原有内存实现的一致性（状态机、提醒预览、时间线等）。

## Implementation Steps
1. **抽取共享逻辑**
   - 将 InMemoryPlanService 中的领域转换、提醒计算、时间线生成等纯函数提取到可复用的 helper（例如 PlanDomainSupport）。
   - 为通知触发、审计记录建立接口或策略，使内存与持久化实现共用。
2. **创建 PersistencePlanService**
   - 以 PlanRepository、PlanAnalyticsRepository、TagRepository、CustomerRepository 等作为依赖，通过 Spring @ConditionalOnBean(PlanAggregateMapper.class) 激活。
   - 实现 listPlans/getPlan 使用 PlanAggregateMapper 聚合结果；createPlan/updatePlan/publish/cancel 等写操作调用 mapper 的 insert/update/delete。
   - 维护节点、提醒、附件的持久化操作（已有 mapper 方法 insertNodes, insertReminderRules 等）。
3. **事务与审计**
   - 在写操作方法上使用 @Transactional，确保 Plan、Node、Reminder、Audit 的跨表操作要么全部成功要么回滚。
   - 审计写入沿用现有 AuditRecorder，在持久化操作成功后记录快照。
4. **通知调用**
   - 将通知触发逻辑提取为 NotificationGateway 接口；InMemoryPlanService 和新的 PersistencePlanService 共用实现，内部根据配置调用 ApiNotificationAdapter/EmailNotificationAdapter 等。
5. **迁移/选择逻辑**
   - 在 Spring 配置中根据 PlanAggregateMapper Bean 是否存在决定加载 PersistencePlanService 或 InMemoryPlanService（保留现有行为用于无数据库的单元测试）。
   - README 与部署文档同步更新，说明如何启用持久化实现。
6. **回归测试**
   - 新增服务级别集成测试（使用 Testcontainers + Mock 通知/审计）验证关键场景：创建/发布/取消、节点执行、提醒更新、PlanBoard/Analytics。
   - API 层（MockMvc）针对 /plans 系列接口补充持久化组合测试。

## Exit Criteria
- 所有 PlanService API 均由持久化实现支撑，InMemory 版本仅保留测试/无数据库模式。
- Testcontainers 套件覆盖：计划 CRUD + 节点执行 + Reminders + Analytics + Board。
- README / Runbook 更新，记录启用持久化、数据库迁移、回退策略。
- CI Pipeline 执行 mvn verify 时默认走持久化实现（需配置 PostgreSQL Testcontainers）。
