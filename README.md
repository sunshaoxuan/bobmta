# BOB MTA Maintain Assistants 平台 - 阶段四持久化与集成基线

本仓库根据《BOB MTA（Maintain Assistants）综合运维平台 详细设计说明书》分阶段实现平台。阶段四聚焦于将内存实现迁移到可持久化架构、铺设仓储抽象，并为即将到来的数据库与外部服务集成做好准备。本轮开始在不影响业务契约的前提下分离存储层，让后续引入 MyBatis、PostgreSQL 及对象存储时能够平滑切换。

## 前后端协同规范

### 文档与沟通流程
- 本仓库 README 作为阶段性基线文档，前后端需在此同步迭代进度，沿用「✅ 已完成 / 🔄 正在进行 / ⏭️ 下一步」结构说明目标、里程碑与依赖。
- 若前端对后端提出新能力需求，必须在 `frontend/FRONTEND_REQUIREMENTS.md` 中创建或更新条目，并补充功能场景、接口需求、数据范围及预期的后端交付阶段。
- 对于尚未交付的后端能力，前端需在对应条目中声明 Mock 策略与验证范围；待后端完成实现并更新状态后，再执行模拟联调与回归，最终在清单中标记「前端完成」。
- 需求沟通或接口变更说明仅限 Markdown 文档（例如 `docs/backend-requests/*.md`），确保版本可追溯；提交前需在 README 的协同章节中引用或链接关键文档。

### 前端研发约束
- 严格遵循《[多语言架构规范](./MULTILINGUAL_GUIDE.md)》，禁止在组件、样式与测试中硬编码语种相关文案，所有文案需通过多语言资源解析。
- 接口联调仅在需求清单标记后端已交付的前提下进行；未交付阶段必须通过 Mock 数据完成单元测试与 Storybook/Playwright 等可视化验证（若适用）。
- 代码提交需附带相应的进度同步与测试记录，前端开发计划维护在 `frontend/FRONTEND_ROADMAP.md`，并在 README 中给出摘要。
- 若前端改动需要后端提供额外配置、权限或数据口径，应同步更新需求清单并创建独立的后端需求说明文件，文内需覆盖背景、场景、契约与验证方式。
- 鉴于当前内网环境无法访问公共 NPM 仓库，前端依赖已预置在 `frontend/vendor` 并通过相对路径引用；执行 `npm install` 仅会生成锁文件而不会触发外网下载，开发者需在拉取最新代码后运行 `npm run build` 验证离线依赖可正常编译，详细流程见《[Frontend Offline Environment Setup](./frontend/ENVIRONMENT_SETUP.md)》。

### 后端配合事项
- 后端完成接口或数据能力后，应在 `frontend/FRONTEND_REQUIREMENTS.md` 的相应条目标记交付阶段，并补充接口契约或样例数据链接。
- 若后端接口调整影响已交付的前端功能，需要在 README 中追加「变更通告」小节说明影响范围、兼容策略与迁移时间窗口。

## 后端阶段四迭代进度

### 🌐 多语言策略
- 产品默认语言为 **日文（ja-JP）**，并提供 **中文（zh-CN）** 可选语言包。
- 全链路多语言设计（资源加载、数据存储、前端缓存、接口契约等）详见《[多语言架构规范](./MULTILINGUAL_GUIDE.md)》。
- 所有研发活动必须遵守上述规范，禁止在代码与测试中写入具有语种特征的字面量。

### 📦 数据加载约束
- 仓储在读取数据时应尽量合并查询以减少往返，但面对潜在的大数据量必须提供分页、最大返回量及防止过量读取的保护策略；一旦超出上限，应返回清晰的错误或指引调用方重试并附带翻页参数。

### 🛠️ 数据库迁移与初始化
- Spring Boot 在检测到数据源后会根据 `application.yml` 中的 Flyway 配置自动执行位于 `classpath:db/migration` 的迁移脚本（已开启 `baseline-on-migrate` 与 `clean-disabled` 防止误清库）。
- 首次拉起 PostgreSQL 后，可通过 `./mvnw -pl backend -DskipTests flyway:migrate`（或 `mvn -pl backend flyway:migrate`）手动重放迁移，确保本地/测试环境结构与生产一致。
- 若需要手动建库或在容器外初始化，可直接运行 `backend/src/main/resources/db/schema.sql` 与 `backend/src/main/resources/db/data.sql`，`deploy/postgres/schema.sql`/`data.sql` 也提供了调用同一脚本的快捷入口。

### ✅ 已完成
- 迭代 #1：建立运维计划仓储抽象，支撑持久化改造的统一入口。
  - 引入 `PlanRepository` 接口与 `InMemoryPlanRepository` 实现，统一计划、节点、提醒策略的存取与 ID 生成逻辑。
  - 重构 `InMemoryPlanService` 依赖仓储接口，移除内部自管的并发容器，让业务逻辑与持久化实现解耦。
  - 同步更新计划控制层、服务层与标签模块单测依赖，并新增仓储单测覆盖 ID 生成与存储行为，确保编译期校验通过。
- 迭代 #2：完成计划模块及审计文案的多语言资源化，确保阶段三能力在国际化架构下稳定演进。
  - 引入 `MessageResolver` 统一封装 Spring `MessageSource`，在计划服务与各控制器中按请求语言解析审计、时间线与错误信息。
  - 将计划时间线、提醒策略默认值、ICS 描述及用户、模板、文件、标签等模块的审计描述迁移到 `i18n/messages` 资源文件，并补充中日双语翻译与单测注入。
- 迭代 #3：增强计划列表筛选能力，为后续仓储查询与索引设计提供接口契约。
  - `/api/v1/plans` 增加负责人与关键字过滤参数，可按计划负责人和标题/描述关键词组合筛选执行队列。
  - `PlanService` 与内存实现支持按 owner/keyword 过滤并保留时间窗口、状态筛选逻辑，为切换数据库查询提前校验业务规则。
  - 控制层、服务层单测新增过滤用例，覆盖多语言环境下的匹配场景，保证迭代编译通过。
- 迭代 #4：固化运维计划仓储查询契约，推进负责人/关键字筛选向持久化实现对齐。
  - 新增 `PlanSearchCriteria` 封装租户、客户、负责人、关键字与时间窗口等过滤条件，作为仓储查询统一入口。
  - `PlanRepository` 提供 `findByCriteria` 能力，内存实现下沉 owner/keyword/时间筛选逻辑，为 MyBatis 查询映射预热。
  - 计划服务与分析能力改用仓储查询结果，保持排序与统计行为一致，并补充仓储层过滤单测确保契约稳定。
- 迭代 #5：交付计划聚合 MyBatis 持久化骨架，串联数据库实体映射与类型处理器。
  - 设计 `mt_plan` 及关联表的字段映射 XML，覆盖计划、节点、执行、附件、活动、提醒策略的 CRUD 操作与分页筛选。
  - 生成 `PlanAggregateMapper` 对应的 MyBatis XML 并注册仓储扫描配置，为 `PlanPersistencePlanRepository` 提供真实数据源桥接。
  - 实现 JSON 列表/字典类型处理器，支撑提醒渠道、活动属性等结构化数据的序列化，并补充单元测试验证序列化健壮性。
- 迭代 #6：区分内存仓储与持久化实现的职责边界，并启动前端计划总览视图。
  - 新增 `PlanSeedDataInitializer` 仅在内存仓储环境注入示例计划，避免持久化数据库被默认数据污染，同时补充启动级单元测试。
  - `InMemoryPlanRepository` 仅在缺失 MyBatis 映射时装配，与 `PlanPersistencePlanRepository` 条件互斥，确保持久化实现优先生效。
  - 前端交付登录表单与计划列表面板，利用多语言资源展示计划状态、时间窗与进度，并支持手动刷新。
- 迭代 #7：统一计划列表分页与查询契约，为 MyBatis 与前端页面提供一致的数据入口。
  - `PlanSearchCriteria` 扩展 limit/offset，仓储新增 `countByCriteria`，MyBatis XML 提供 `countPlans` 以支撑数据库统计。
  - `PlanService` 返回 `PlanSearchResult`，控制层直接使用仓储分页结果组装 `PageResponse`，避免内存子集分页的总数误差。
  - 内存仓储与持久化实现统一按照计划开始时间排序并应用分页裁剪，补充单元测试覆盖页码、统计和顺序一致性。
- 迭代 #8：梳理计划服务的事务边界，为持久化切换提供一致的写入顺序。
  - 在计划服务上引入默认的只读事务配置，确保查询接口在未来接入 MyBatis 时具备统一的会话隔离。
  - 为创建、更新、发布、取消、节点执行、交接及提醒策略等写操作显式声明事务，保障聚合写入与审计扩展具备原子性。
  - 同步更新阶段进度描述，记录事务治理里程碑并为后续的 MyBatis 联调与索引规划铺路。
- 迭代 #9：构建计划统计仓储并联通 MyBatis 聚合查询，降低分析接口的运行成本。
  - 抽象 `PlanAnalyticsRepository` 以及内存/持久化实现，将计划状态统计、逾期计算与即将开始的计划聚合下沉到仓储层统一处理。
  - 扩展 `PlanAggregateMapper` 与 XML，在数据库侧汇总状态分布、逾期数量及节点完成度，返回可直接映射的统计实体。
  - 调整计划服务与相关单测依赖新的统计仓储，保持接口契约不变的同时减少业务层重复计算和排序逻辑。
- 迭代 #10：细化计划驾驶舱的筛选能力，支撑不同客户视角的风险排查。
  - `/api/v1/plans/analytics` 新增 `customerId` 查询参数，计划服务统一封装为 `PlanAnalyticsQuery` 传递给仓储层。
  - `PlanAnalyticsRepository` 的内存版与 MyBatis 版均支持客户筛选，统计查询会将 `customer_id` 条件下沉到 SQL 与内存过滤器。
  - 新增计划服务与仓储单元测试覆盖客户过滤逻辑，确保状态汇总与即将开始计划列表在不同实现间保持一致。
- 迭代 #11：落实计划列表的租户隔离，防止跨租户数据串扰。
  - `/api/v1/plans` 支持可选的 `tenantId` 查询参数，控制层将租户上下文传入服务层过滤。
  - 计划服务扩展 `PlanSearchCriteria` 传递租户条件，统一分页、计数与计划聚合的租户维度约束。
  - 内存实现与控制层单测新增租户过滤用例，验证不同租户的计划不会出现在其他租户的列表视图中。
- 迭代 #12：实现多语言文本的持久化仓储，为阶段四的数据落库提供支撑。
  - 基于 MyBatis 定义 `mt_multilingual_texts` 映射，提供多语言值的查询、更新与插入能力。
  - 新增 `PersistenceMultilingualTextRepository`，以数据库优先生效、内存实现作为降级方案。
  - 通过单元测试覆盖插入、更新与读取路径，确保多语言资源在不同存储介质间行为一致。
- 迭代 #13：让系统默认语言配置持久化，保证多实例部署时语言偏好一致。
  - 定义 `LocaleSettingsMapper` 及 `mt_locale_settings` 映射，存在数据源时自动装配持久化仓储实现。
  - `LocalePreferenceService` 通过统一仓储接口读取/更新默认语言，内存仓储仅在缺失 MyBatis 映射时启用。
  - 为持久化仓储补充单元测试，覆盖空值、更新与插入分支并验证语言标签的标准化写入。
- 迭代 #14：交付系统默认语言维护接口，支持管理员在符合规范的前提下更新多语言偏好。
  - 新增 `/api/v1/i18n/default-locale` 接口，返回当前默认语言与受支持语种列表，并强制管理员权限执行更新。
  - `LocalePreferenceService` 校验语种是否受支持、提供持久化写入，并在控制层与服务层单测中验证成功与失败分支。
- 迭代 #15：扩展计划节点执行动作与阈值处理，支撑前端对可选任务的灵活控制。
  - 为节点模型补充 `actionType` 与 `completionThreshold` 字段，覆盖远程/邮件/IM/链接/文件等动作类型并约束执行阈值范围。
  - 完成节点时根据阈值自动补齐父节点完成并跳过剩余子节点，时间线记录 `plan.activity.nodeAutoCompleted` 与 `plan.activity.nodeSkipped` 事件。
  - 更新控制层、服务层与持久化映射及单元测试，验证阈值触发后的执行状态、计划完结与多语言文案输出一致。
- 迭代 #16：交付时间线事件字典接口，完善前端 F-002 的展示所需语义。
  - 新增 `GET /api/v1/plans/activity-types`，汇总 `PlanActivityType` 支持的消息键及属性描述，响应随请求语言返回多语言文案。
  - 梳理时间线事件产生的属性并补充中日双语说明，方便前端根据 `descriptionKey` 自定义渲染或筛选图标。
  - 在《docs/backend-requests/plan-timeline-activities.md》记录事件与属性对照，单元测试覆盖字典接口。

- 迭代 #15：扩展计划节点执行动作与阈值处理，支撑前端对可选任务的灵活控制。
  - 为节点模型补充 `actionType` 与 `completionThreshold` 字段，覆盖远程/邮件/IM/链接/文件等动作类型并约束执行阈值范围。
  - 完成节点时根据阈值自动补齐父节点完成并跳过剩余子节点，时间线记录 `plan.activity.nodeAutoCompleted` 与 `plan.activity.nodeSkipped` 事件。
  - 更新控制层、服务层与持久化映射及单元测试，验证阈值触发后的执行状态、计划完结与多语言文案输出一致。
- 迭代 #16：交付时间线事件字典接口，完善前端 F-002 的展示所需语义。
  - 新增 `GET /api/v1/plans/activity-types`，汇总 `PlanActivityType` 支持的消息键及属性描述，响应随请求语言返回多语言文案。
  - 梳理时间线事件产生的属性并补充中日双语说明，方便前端根据 `descriptionKey` 自定义渲染或筛选图标。
  - 在《docs/backend-requests/plan-timeline-activities.md》记录事件与属性对照，单元测试覆盖字典接口。
- 迭代 #17：输出提醒策略配置字典接口，支撑前端构建提醒策略编辑器。
  - 提供 `GET /api/v1/plans/reminder-options` 返回触发时机、通知渠道、收件人群组及偏移范围，并带多语言说明。
  - 在服务层抽象提醒配置描述符，统一维护触发枚举、渠道代号与默认偏移，方便后续扩展 SMS/IM 以外的渠道。
  - 更新中日双语资源及控制层单元测试，确保字典接口按照请求语言返回正确文案，并在《docs/backend-requests/plan-reminder-options.md》记录契约。

- 迭代 #15：扩展计划节点执行动作与阈值处理，支撑前端对可选任务的灵活控制。
  - 为节点模型补充 `actionType` 与 `completionThreshold` 字段，覆盖远程/邮件/IM/链接/文件等动作类型并约束执行阈值范围。
  - 完成节点时根据阈值自动补齐父节点完成并跳过剩余子节点，时间线记录 `plan.activity.nodeAutoCompleted` 与 `plan.activity.nodeSkipped` 事件。
  - 更新控制层、服务层与持久化映射及单元测试，验证阈值触发后的执行状态、计划完结与多语言文案输出一致。
- 迭代 #16：交付时间线事件字典接口，完善前端 F-002 的展示所需语义。
  - 新增 `GET /api/v1/plans/activity-types`，汇总 `PlanActivityType` 支持的消息键及属性描述，响应随请求语言返回多语言文案。
  - 梳理时间线事件产生的属性并补充中日双语说明，方便前端根据 `descriptionKey` 自定义渲染或筛选图标。
  - 在《docs/backend-requests/plan-timeline-activities.md》记录事件与属性对照，单元测试覆盖字典接口。
- 迭代 #17：输出提醒策略配置字典接口，支撑前端构建提醒策略编辑器。
  - 提供 `GET /api/v1/plans/reminder-options` 返回触发时机、通知渠道、收件人群组及偏移范围，并带多语言说明。
  - 在服务层抽象提醒配置描述符，统一维护触发枚举、渠道代号与默认偏移，方便后续扩展 SMS/IM 以外的渠道。
  - 更新中日双语资源及控制层单元测试，确保字典接口按照请求语言返回正确文案，并在《docs/backend-requests/plan-reminder-options.md》记录契约。
- 迭代 #18：交付计划列表筛选字典，为 F-001 的筛选面板提供动态多语言数据。
  - 新增 `GET /api/v1/plans/filter-options`，返回计划状态、负责人、客户的候选项及当前数量，并附带预计时间窗建议。
  - 服务层按照租户过滤计划数据，统一以多语言消息渲染标签，并在控制层单测校验中文环境下的标签与数量。
  - 在《docs/backend-requests/plan-filter-options.md》记录契约，同时更新多语言资源，供前端替换静态筛选枚举。

- 迭代 #15：扩展计划节点执行动作与阈值处理，支撑前端对可选任务的灵活控制。
  - 为节点模型补充 `actionType` 与 `completionThreshold` 字段，覆盖远程/邮件/IM/链接/文件等动作类型并约束执行阈值范围。
  - 完成节点时根据阈值自动补齐父节点完成并跳过剩余子节点，时间线记录 `plan.activity.nodeAutoCompleted` 与 `plan.activity.nodeSkipped` 事件。
  - 更新控制层、服务层与持久化映射及单元测试，验证阈值触发后的执行状态、计划完结与多语言文案输出一致。
- 迭代 #16：交付时间线事件字典接口，完善前端 F-002 的展示所需语义。
  - 新增 `GET /api/v1/plans/activity-types`，汇总 `PlanActivityType` 支持的消息键及属性描述，响应随请求语言返回多语言文案。
  - 梳理时间线事件产生的属性并补充中日双语说明，方便前端根据 `descriptionKey` 自定义渲染或筛选图标。
  - 在《docs/backend-requests/plan-timeline-activities.md》记录事件与属性对照，单元测试覆盖字典接口。
- 迭代 #17：输出提醒策略配置字典接口，支撑前端构建提醒策略编辑器。
  - 提供 `GET /api/v1/plans/reminder-options` 返回触发时机、通知渠道、收件人群组及偏移范围，并带多语言说明。
  - 在服务层抽象提醒配置描述符，统一维护触发枚举、渠道代号与默认偏移，方便后续扩展 SMS/IM 以外的渠道。
  - 更新中日双语资源及控制层单元测试，确保字典接口按照请求语言返回正确文案，并在《docs/backend-requests/plan-reminder-options.md》记录契约。
- 迭代 #18：交付计划列表筛选字典，为 F-001 的筛选面板提供动态多语言数据。
  - 新增 `GET /api/v1/plans/filter-options`，返回计划状态、负责人、客户的候选项及当前数量，并附带预计时间窗建议。
  - 服务层按照租户过滤计划数据，统一以多语言消息渲染标签，并在控制层单测校验中文环境下的标签与数量。
  - 在《docs/backend-requests/plan-filter-options.md》记录契约，同时更新多语言资源，供前端替换静态筛选枚举。
- 迭代 #19：扩展计划驾驶舱统计的负责人负载与风险洞察，支撑 F-004 的图表与提醒提示。
  - `/api/v1/plans/analytics` 响应新增负责人负载与风险计划列表，按照多租户过滤聚合活跃/逾期数量，并限制返回数量。
  - 内存与持久化分析仓储统一计算即将到期与已逾期计划，按照 24 小时窗口标记风险等级，并在控制层/服务层单测覆盖。
  - 新增《docs/backend-requests/plan-analytics-dashboard.md》说明驾驶舱数据结构，便于前端联调图表与风险提示。

- 迭代 #15：扩展计划节点执行动作与阈值处理，支撑前端对可选任务的灵活控制。
  - 为节点模型补充 `actionType` 与 `completionThreshold` 字段，覆盖远程/邮件/IM/链接/文件等动作类型并约束执行阈值范围。
  - 完成节点时根据阈值自动补齐父节点完成并跳过剩余子节点，时间线记录 `plan.activity.nodeAutoCompleted` 与 `plan.activity.nodeSkipped` 事件。
  - 更新控制层、服务层与持久化映射及单元测试，验证阈值触发后的执行状态、计划完结与多语言文案输出一致。
- 迭代 #16：交付时间线事件字典接口，完善前端 F-002 的展示所需语义。
  - 新增 `GET /api/v1/plans/activity-types`，汇总 `PlanActivityType` 支持的消息键及属性描述，响应随请求语言返回多语言文案。
  - 梳理时间线事件产生的属性并补充中日双语说明，方便前端根据 `descriptionKey` 自定义渲染或筛选图标。
  - 在《docs/backend-requests/plan-timeline-activities.md》记录事件与属性对照，单元测试覆盖字典接口。
- 迭代 #17：输出提醒策略配置字典接口，支撑前端构建提醒策略编辑器。
  - 提供 `GET /api/v1/plans/reminder-options` 返回触发时机、通知渠道、收件人群组及偏移范围，并带多语言说明。
  - 在服务层抽象提醒配置描述符，统一维护触发枚举、渠道代号与默认偏移，方便后续扩展 SMS/IM 以外的渠道。
  - 更新中日双语资源及控制层单元测试，确保字典接口按照请求语言返回正确文案，并在《docs/backend-requests/plan-reminder-options.md》记录契约。
- 迭代 #18：交付计划列表筛选字典，为 F-001 的筛选面板提供动态多语言数据。
  - 新增 `GET /api/v1/plans/filter-options`，返回计划状态、负责人、客户的候选项及当前数量，并附带预计时间窗建议。
  - 服务层按照租户过滤计划数据，统一以多语言消息渲染标签，并在控制层单测校验中文环境下的标签与数量。
  - 在《docs/backend-requests/plan-filter-options.md》记录契约，同时更新多语言资源，供前端替换静态筛选枚举。
- 迭代 #19：扩展计划驾驶舱统计的负责人负载与风险洞察，支撑 F-004 的图表与提醒提示。
  - `/api/v1/plans/analytics` 响应新增负责人负载与风险计划列表，按照多租户过滤聚合活跃/逾期数量，并限制返回数量。
  - 内存与持久化分析仓储统一计算即将到期与已逾期计划，按照 24 小时窗口标记风险等级，并在控制层/服务层单测覆盖。
  - 新增《docs/backend-requests/plan-analytics-dashboard.md》说明驾驶舱数据结构，便于前端联调图表与风险提示。
- 迭代 #20：为驾驶舱统计增加负责人过滤能力，便于前端聚焦单个执行人视角分析。
  - `/api/v1/plans/analytics` 新增 `ownerId` 查询参数，可在指定租户/客户的基础上进一步筛选负责人范围。
  - 内存与持久化统计仓储下沉负责人条件，确保状态计数、即将开始计划、负责人负载与风险计划均遵循过滤范围。
  - 扩展控制层与服务层单元测试覆盖负责人过滤场景，保证返回的字典及统计仅包含目标负责人。

### 🔄 正在进行
- 基于 `PlanSearchCriteria` 细化数据库层的字段映射与索引规划，评估多维组合筛选的 SQL 与分页策略。
- 联调 MyBatis 映射与数据源配置，完善序列生成、分页与并发写入策略，确保切换数据库后仓储读写一致。
- 构建前端运维计划界面的状态管理与交互流程，补齐登录态缓存、错误提示与多语言资源加载的联合测试。

### ⏭️ 下一步
- 输出基于 PostgreSQL 的初始化脚本与迁移策略，为 MyBatis `PlanRepository` 切换真实数据源做好准备。
- 让提醒策略、时间线与附件模块共用新的仓储抽象，完成计划模块持久化改造的闭环。
- 推进对象存储、消息通知等外部集成的配置抽象，准备阶段四后续迭代的集成联调，并补齐剩余模块的多语言资源治理。
- 在仓储实现阶段引入基于负责人/关键字过滤的集成测试，验证 SQL 与多语言消息解析的一致性。
- 扩展前端计划列表的筛选、详情导航与提醒配置操作，沉淀认证态缓存与接口契约的端到端验证。

### 🗄️ 数据库迁移与初始化指南
- `backend/src/main/resources/db/schema.sql` 与 `data.sql` 覆盖计划、节点、执行、提醒、附件、活动及文件元数据等核心表结构，并同步声明多维筛选所需的复合索引。
- `deploy/postgres/schema.sql` / `data.sql` 作为部署入口的包装脚本，支持在宿主机上通过 `psql -f deploy/postgres/schema.sql` 与 `psql -f deploy/postgres/data.sql` 快速重建数据库。
- Spring Boot 默认启用 Flyway（`spring.flyway.enabled=true`），应用启动或测试时会自动执行 `db/migration` 目录下的版本化脚本，保持 schema 与索引演进。
- 若需单独校验数据库迁移，可运行 `mvn -f backend/pom.xml test`（需本地已缓存 Spring Boot 依赖或配置私有仓库镜像），集成测试会基于 Testcontainers 的 PostgreSQL 自动拉起数据库、执行 Flyway 迁移，并验证多维筛选、统计与事务一致性。
- `backend/src/test/java/com/bob/mta/modules/plan/persistence/PlanAggregateMapperIntegrationTest.java` 覆盖运维计划的多维筛选、统计聚合、事务回滚等 SQL 契约，新增加的关键字/排除筛选用例可帮助验证索引是否生效。

## 前端阶段迭代进度

### ✅ 已完成
- 迭代 #0：交付登录表单与计划列表面板，完成多语言资源、后端健康检查展示及计划概要表格渲染，奠定国际化与鉴权流程基础。
- 迭代 #1：完成前后端协同规范、需求清单与路线图编写，并交付离线依赖基线，为后续状态管理与联调流程提供制度保障。

### 🔄 正在进行
- 迭代 #2：搭建状态管理与请求封装，梳理鉴权、计划列表与多语言的共享上下文，逐步替换页面内散落的网络请求逻辑。
  - ✅ 已完成：构建 API 客户端抽象、Session/PlanList 上下文与统一错误描述方法，登录与计划列表均由状态容器驱动，刷新/登出后自动回收缓存。
  - ✅ 已完成：建立计划列表与会话的 Mock 数据集，并通过 Node Test 驱动的单元测试校验 API 客户端的错误映射与 Mock 过滤分页逻辑。
  - ✅ 已完成：抽象通用的加载/空状态/错误呈现组件并交付计划列表筛选表单骨架，可按负责人、状态、关键字组合筛选并复用 Mock 校验。
  - ✅ 已完成：补齐计划筛选的时间窗口与分页治理，提供前端分页控件、离线日期选择器，并以 Mock 测试覆盖时间窗口交集规则。
  - ✅ 已完成：实现分页结果缓存与计划概要索引，命中缓存时提示数据来源并展示最近更新时间，为计划详情与离线浏览提供数据基础。
  - ✅ 已完成：基于缓存索引交付计划详情预览骨架，自动联动列表选中项并展示离线摘要，为后续详情接口与时间线联调夯实 UI 基线。
  - ✅ 已完成：梳理计划详情数据模型与 Mock 详情接口，补充时间线/提醒占位视图并引入分页缓存回收策略，详情面板支持手动刷新与缓存命中提示。
  - ✅ 已完成：提炼计划详情缓存工具与 TTL/淘汰策略，补充 Node Test 验证缓存保留/刷新分支，并修复 `npm run build` `TS1128` 报错恢复构建链路。
  - ✅ 已完成：统一计划详情、时间线与提醒 payload 的字段归一化，规范标签、参与人与节点层级排序后再写入缓存，避免多源数据导致的渲染偏差。
  - ✅ 已完成：交付计划节点树形概览，复用多语言标签标注顺序、执行人、时长与结果信息，并与提醒/时间线视图保持一致的错误与加载反馈。
  - ✅ 已完成：梳理节点执行与提醒配置的交互占位，新增详情分区通用组件、节点操作面板与提醒卡片交互，按钮行为以本地状态模拟并在需求清单登记后端接口诉求。
  - ✅ 已完成：依据计划状态区分设计/执行模式，执行态高亮当前节点并锁定已完成节点，模式面板提示切换回设计态；PlanPreview 按 `PlanDetail.status` 映射模式标签并向节点树、提醒看板与操作面板传递模式控制编辑，设计态继续开放节点编辑入口且提醒编辑按钮仅在设计态可见，执行态自动收起节点/提醒编辑态；同步状态容器暴露模式与当前节点信息，并新增单元测试覆盖取消态、设计↔执行切换、回退到草稿与嵌套节点执行场景，同时拆分模式提示面板以分别渲染设计/执行提示、弱化执行态已完成节点并补充模式/活跃节点选择器与切换回退用例。
  - ✅ 已完成：重构全局 `HeaderNav` 组件，使用 `Layout.Header` + `Menu` + `Dropdown` 承载品牌区、角色敏感导航与用户菜单，新增固定吸顶布局；Session 状态同步暴露导航配置、用户菜单与角色信息，组件内部按角色过滤菜单并在接口异常时降级为 Mock。未登录场景仅保留访客徽标与登录按钮，不再渲染导航项；命中 403 会在导航栏提示权限告警并保留 Mock 菜单回退，401 自动清理会话并提示重新登录入口。
  - ✅ 已完成：在鉴权态下校验前端路由授权，未匹配的路径会展示 403 提示与返回概览按钮，避免用户访问未开放的页面时出现空白视图。
  - ✅ 已完成：升级计划多视图为 Tabs + Segmented 联动切换，沿用筛选条件渲染表格、客户树形列表与日历视图，客户视图以 List/Tree 呈现负责人与状态分布，日历视图接入事件映射与多粒度聚合，并在即将开始列表中过滤出基于时间锚点的未来事件；PlanListBoard 将 `Segmented` 与 `Tabs` 共用的 `viewMode` 状态同步写回 URL，Node Test 覆盖客户聚合、日历分组与时间锚点推断逻辑。
  - ✅ 已完成：封装 `PlanByCustomerView` 与 `PlanCalendarView` 组件复用计划聚合结果，客户视图展示状态树与负责人标签，日历视图支持日/周/月/年颗粒度分桶并联动列表渲染；配套在 `planList` 状态模块输出客户聚合与日历事件派生方法，并以 Node Test 校验排序、周起始日与年度区间边界。
  - 📌 下一步：等待后端提供节点执行与提醒更新接口后对接真实调用，并补齐操作失败提示与权限校验的前端展现。
  - ✅ 已确认：F-001/F-002 依赖的后端字典接口已在 2025-09-29 发布并进入生产环境，对应契约、`curl` 示例与返回片段已同步回前端需求清单供联调核对。
    - `GET /api/v1/plans/filter-options`：契约详见《[计划列表筛选字典接口说明](docs/backend-requests/plan-filter-options.md#响应结构)》，可通过 `curl -H "Accept-Language: zh-CN" "${HOST}/api/v1/plans/filter-options?tenantId=acme"` 验证多语言标签与时间窗示例。
    - `GET /api/v1/plans/activity-types`：契约详见《[计划时间线事件字典说明](docs/backend-requests/plan-timeline-activities.md#响应结构)》，可通过 `curl -H "Accept-Language: ja-JP" "${HOST}/api/v1/plans/activity-types"` 核对消息键与属性描述。
    - Mock 仅在离线/测试环境或接口异常时回退到 `mockPlanFilterOptions.json`/`mockPlanActivityTypes.json`，持续镜像 2025-09 契约字段并通过 Node Test 校验筛选枚举排序、时间窗提示与时间线属性渲染，同时记录真实接口联调下的缓存刷新与降级策略。

### ⏭️ 下一步
- 迭代 #2：设计前端状态管理与缓存方案，区分用户会话、计划查询缓存与多语言资源加载，完善 Mock 数据与单元测试基线。
- 迭代 #3：扩展计划列表筛选、详情导航与提醒策略配置的页面结构，准备与后端分页与筛选契约的联调测试。
- 迭代 #4：补齐全局错误处理、权限提示与多语言文案回退策略，并引入可复用的请求封装与 Loading/Empty/Retry 组件。

## 后端阶段三交付回顾

### ✅ 完成清单
- 迭代 #1：为用户管理接口补齐审计日志与管理员守卫，保障敏感操作可追踪。
- 迭代 #2：重构创建/激活用户返回结构，补充激活链接透出与服务契约对齐。
- 迭代 #3：梳理客户、自定义字段、标签控制器依赖，统一实体校验与内存实现。
- 迭代 #4：实现运维计划的全生命周期操作、节点执行流与 ICS 导出能力。
- 迭代 #5：记录计划取消原因/操作人/时间，在详情、列表及 ICS 中暴露，并新增控制层、服务层单测验证。
- 迭代 #6：收紧运维计划执行状态校验，补全审计前后镜像，并扩展边界用例测试。
  - 限制计划仅在已发布且处于活动状态时才能启动或完成节点，阻断设计态及已取消计划的误操作。
  - 在发布、取消与节点执行接口中记录审计前后快照，保留状态和执行信息的完整变更轨迹。
  - 扩充计划服务与控制器单测，覆盖未发布/已取消计划及节点未启动即完成等边界场景。
- 迭代 #7：构建运维计划时间线服务，串联计划发布、节点执行、完成等关键活动。
  - 为计划领域模型新增活动轨迹，记录创建、发布、取消、节点执行与计划完成等事件细节。
  - 暴露 `/api/v1/plans/{id}/timeline` API 及详情响应时间线，便于前端渲染执行追踪视图。
  - 更新计划控制器与服务单元测试，验证时间线覆盖节点生命周期及完结场景，完成阶段三交付清单。
- 迭代 #8：完善运维计划提醒策略与多渠道通知能力，提供统一的策略配置与预览服务。
  - 在计划详情中返回提醒策略摘要，并为管理端提供 `/api/v1/plans/{id}/reminders` 查询与更新接口。
  - 预置默认的邮件、短信、IM 组合规则，支持管理员按触发时机、渠道与模板自定义提醒策略。
  - 新增 `/api/v1/plans/{id}/reminders/preview` 接口，按基准时间计算未来提醒时间点，支撑前端日程与通知视图。
- 迭代 #9：打通运维计划节点与文件服务，补齐执行证据的多维透出能力，完成阶段三交付收尾。
  - 为节点执行响应增加附件清单，携带文件名、类型、大小与下载地址，便于前端直接渲染凭证。
  - `GET /api/v1/plans/{id}` 详情与节点执行接口在返回值中补充附件元数据，审计记录也同步捕获新增快照。
  - 调整控制器单测覆盖上传场景，校验附件下载地址与审计日志写入，完成计划模块与文件模块的内聚。
- 迭代 #10：构建运维计划驾驶舱分析视图，提供状态分布与逾期风险洞察。
  - 新增 `/api/v1/plans/analytics` 接口，支持按租户与时间范围聚合计划状态、逾期数量与未来执行队列。
  - 计划服务补充统计模型，返回节点进度、负责人等关键信息，支撑控制台驾驶舱渲染。
  - 扩展控制层与服务层单元测试覆盖分析场景，验证取消、发布、逾期等状态变更会实时反映在统计结果中。
- 迭代 #11：交付运维计划负责人交接能力，支持执行过程中的安全移交。
  - 新增 `/api/v1/plans/{id}/handover` 接口，可指定新负责人、同步参与人并写入交接备注。
  - 计划服务新增交接校验，阻止已结束计划重复移交，并将交接详情写入时间线活动。
  - 扩充控制器与服务层单测，验证交接后时间线、审计记录与参与人列表保持一致。

### 🔄 后续跟进
- 阶段三交付内容已全部完成，后续持续优化与集成工作将在阶段四迭代中推进。

### ⏭️ 后续规划
- 阶段三无遗留需求，所有增量均已纳入阶段四路线图。

## 项目结构

```
backend/   # Spring Boot 3 后端服务，聚合阶段三/四功能所需的 REST API 与仓储抽象
frontend/  # React + Vite 前端占位，后续阶段将继续完善
deploy/    # Docker Compose 所需的 Nginx/后端/MinIO/TLS 示例配置
```

## 部署与运行

设计说明书推荐通过 Docker Compose 一键编排前端、后端、PostgreSQL 与 MinIO。本仓库在根目录提供了与文档结构一致的 `docker-compose.yml` 以及 `deploy/` 目录，可按以下步骤启动：

1. 构建前端静态资源，供 Nginx 挂载：

   ```bash
   cd frontend
   npm install
   npm run build
   cd ..
   ```

2. 根据环境安全要求覆盖默认变量，可直接编辑 `deploy/backend/backend.env` 或复制为 `.env` 并在运行时通过 `--env-file` 指定。关键变量包括：

   | 变量 | 默认值 | 用途 |
   | --- | --- | --- |
   | `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `bobmta` / `bobmta` / `change-me` | 数据库名称与账号 |
   | `JWT_ACCESS_TOKEN_SECRET` | `change-me-please` | 后端 JWT 签名密钥 |
   | `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | `minioadmin` / `minioadmin` | MinIO 初始管理员账号 |
   | `MINIO_BUCKET` | `plan-files` | 计划附件等对象存储桶名称 |
   | `TZ` | `Asia/Tokyo` | 各容器默认时区 |

3. 构建并启动全部服务：

   ```bash
   docker compose up -d --build
   ```

   - 前端静态站点通过 `http://localhost:8080` 访问。
   - 后端 API 映射至 `http://localhost:8081`，供调试或反向代理联通。
   - PostgreSQL 和 MinIO 分别暴露在 `5432`、`9000`/`9001` 端口，可使用本地客户端连线。

4. 若需要统一域名及 TLS，可在 `deploy/certs` 放置 `fullchain.pem`/`privkey.pem` 后，启用代理 Profile：

   ```bash
   docker compose --profile proxy up -d
   ```

   该 `nginx` 容器会读取 `deploy/nginx/reverse-proxy.conf`，将 80 端口重定向至 443 并分别转发 `/` 和 `/api/` 到内部的 `web`、`api` 容器。

常见操作：

- MinIO 控制台：浏览器访问 `http://localhost:9001`，按上表凭证登陆后创建 `plan-files` 桶（或依据需要更改名称并同步更新 `.env`）。
- 数据卷：PostgreSQL 与 MinIO 数据默认保存在 `deploy/postgres/data`、`deploy/minio/data`，证书放置于 `deploy/certs`，可根据实际部署调整为独立挂载路径。
- 停止并清理：执行 `docker compose down`，若需清除数据卷可追加 `-v` 参数。

## 后端快速开始

```bash
cd backend
mvn spring-boot:run
```

应用启动后可尝试以下示例接口：

| 接口 | 描述 | 备注 |
| --- | --- | --- |
| `POST /api/v1/auth/login` | 账号登录（内存账户） | 预置账号：`admin`/`admin123`、`operator`/`operator123` |
| `GET /api/v1/auth/me` | 获取当前登录用户信息 | 需要在 `Authorization: Bearer <token>` 中携带登录返回的 Token |
| `POST /api/v1/users` | 创建系统用户并发放激活链接 | 需管理员角色 |
| `POST /api/v1/users/activation` | 校验激活 Token 并启用账号 | 激活接口对未登录用户开放 |
| `POST /api/v1/users/{id}/activation/resend` | 重新发放激活链接 | 需管理员角色 |
| `PUT /api/v1/users/{id}/roles` | 更新用户角色集合 | 角色名自动标准化为 `ROLE_*` |
| `GET /api/v1/customers` | 客户列表 | 支持按地区与关键字过滤（内存数据） |
| `GET /api/v1/customers/{id}` | 客户详情 | 展示联系人、自定义字段等结构 |
| `GET /api/v1/custom-fields` | 自定义字段定义列表 | 支持动态档案字段配置 |
| `PUT /api/v1/custom-fields/customers/{id}` | 更新客户自定义字段值 | 支持增改非结构化字段 |
| `GET /api/v1/plans` | 运维计划列表 | 支持按客户、状态、时间范围过滤并返回进度摘要 |
| `POST /api/v1/plans` | 创建运维计划 | 接收节点树结构与参与人列表，初始状态为 DESIGN |
| `PUT /api/v1/plans/{id}` | 更新运维计划 | DESIGN 状态下允许调整时间、参与人及节点定义 |
| `DELETE /api/v1/plans/{id}` | 删除运维计划 | 仅 DESIGN 计划可删除，删除时写入审计日志 |
| `POST /api/v1/plans/{id}/publish` | 发布运维计划 | 根据开始时间切换为 SCHEDULED/IN_PROGRESS，并记录审计 |
| `POST /api/v1/plans/{id}/cancel` | 取消运维计划 | 写入取消原因/操作者并终止计划 |
| `POST /api/v1/plans/{id}/nodes/{nodeId}/start` | 开始执行节点 | 切换节点状态为 IN_PROGRESS，返回最新计划详情 |
| `POST /api/v1/plans/{id}/nodes/{nodeId}/complete` | 完成节点 | 校验已启动后方可完成，支持提交结果、日志与附件并返回计划详情 |
| `POST /api/v1/plans/{id}/nodes/{nodeId}/handover` | 节点交接 | 指定新的执行人并写入审计与时间线 |
| `GET /api/v1/plans/{id}` | 运维计划详情 | 展示流程节点树形结构及执行进度 |
| `GET /api/v1/plans/{id}/timeline` | 运维计划时间线 | 返回计划与节点的关键活动轨迹 |
| `GET /api/v1/plans/{id}/reminders` | 查看运维计划提醒策略 | 返回默认及自定义的提醒规则列表 |
| `PUT /api/v1/plans/{id}/reminders` | 更新运维计划提醒策略 | 支持配置渠道、模板、触发时机并记录审计 |
| `PUT /api/v1/plans/{id}/reminders/{reminderId}` | 更新单条提醒规则 | 启停提醒或调整偏移量，返回最新计划详情 |
| `GET /api/v1/plans/{id}/reminders/preview` | 预览运维计划提醒触达计划 | 基于时间窗口计算未来触达时间点 |
| `GET /api/v1/plans/{id}/ics` | 导出单计划 ICS | 生成 `text/calendar` 文件，可导入主流日历 |
| `GET /api/v1/calendar/tenant/{tenant}.ics` | 租户计划订阅 | 输出租户可见计划的 ICS 订阅源 |
| `GET /api/v1/tags` | 标签管理 | 支持按作用域筛选、关联客户/计划 |
| `POST /api/v1/templates/{id}/render` | 模板渲染 | 根据上下文替换占位符，返回渲染结果 |
| `POST /api/v1/files` | 文件元数据登记 | 生成对象存储键及下载地址 |
| `GET /api/v1/audit-logs` | 审计日志查询 | 需管理员角色 |
| `GET /api/ping` | 健康检查 | 返回 `{status: ok}` |

### 测试与覆盖率

```bash
cd backend
mvn verify
```

该命令会运行全部单元测试并在 `backend/target/site/jacoco/index.html` 生成 Jacoco 覆盖率报表。若初次执行无法下载依赖，可根据环境配置 Maven 镜像。

## 下一步计划

- 引入 PostgreSQL + MyBatis 持久化层，实现标签、模板、文件、自定义字段等实体的数据库存储；
- 与前端协同定义 OpenAPI 契约，扩展状态管理与国际化能力；
- 补充更多集成测试并接入 CI，持续维持覆盖率在 80% 以上；
- 推进对象存储与日历订阅等外部集成，完善阶段四及之后的联调准备。
