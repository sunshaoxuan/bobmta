-- -----------------------------------------------------------------------------
-- BOB MTA Maintenance Assistants - PostgreSQL schema baseline
-- -----------------------------------------------------------------------------
-- 迁移策略说明：
-- 1. 所有结构变更必须以 Flyway 版本脚本（classpath:db/migration/V__*.sql）交付。
--    当前仓库以 V1__plan_module.sql 建立计划、节点、提醒等基础表。
-- 2. 后续如需新增字段或索引，请创建新的 V{n}__description.sql 文件，避免修改已发布版本。
-- 3. 若需手动初始化或在测试环境快速重置，可直接执行本文件中的 DDL，并视需要再运行 data.sql。
-- -----------------------------------------------------------------------------

-- 序列定义 --------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS mt_plan_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS mt_plan_node_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS mt_plan_reminder_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

-- 主表：运维计划 --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mt_plan (
    plan_id              VARCHAR(64) PRIMARY KEY,
    tenant_id            VARCHAR(64)    NOT NULL,
    customer_id          VARCHAR(64),
    owner_id             VARCHAR(64),
    title                VARCHAR(255)   NOT NULL,
    description          TEXT,
    status               VARCHAR(32)    NOT NULL,
    planned_start_time   TIMESTAMPTZ,
    planned_end_time     TIMESTAMPTZ,
    actual_start_time    TIMESTAMPTZ,
    actual_end_time      TIMESTAMPTZ,
    cancel_reason        TEXT,
    canceled_by          VARCHAR(64),
    canceled_at          TIMESTAMPTZ,
    timezone             VARCHAR(64)    NOT NULL,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    reminder_updated_at  TIMESTAMPTZ,
    reminder_updated_by  VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_mt_plan_tenant_status ON mt_plan (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_mt_plan_tenant_start ON mt_plan (tenant_id, planned_start_time);
CREATE INDEX IF NOT EXISTS idx_mt_plan_tenant_end ON mt_plan (tenant_id, planned_end_time);
CREATE INDEX IF NOT EXISTS idx_mt_plan_customer_status ON mt_plan (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_mt_plan_owner_status ON mt_plan (owner_id, status);
CREATE INDEX IF NOT EXISTS idx_mt_plan_tenant_owner_end ON mt_plan (tenant_id, owner_id, planned_end_time);
CREATE INDEX IF NOT EXISTS idx_mt_plan_tenant_customer_start ON mt_plan (tenant_id, customer_id, planned_start_time);
CREATE INDEX IF NOT EXISTS idx_mt_plan_tenant_status_end ON mt_plan (tenant_id, status, planned_end_time);
CREATE INDEX IF NOT EXISTS idx_mt_plan_status_end ON mt_plan (status, planned_end_time);

-- 参与者 ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mt_plan_participant (
    plan_id        VARCHAR(64) NOT NULL,
    participant_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (plan_id, participant_id),
    CONSTRAINT fk_plan_participant_plan FOREIGN KEY (plan_id) REFERENCES mt_plan(plan_id) ON DELETE CASCADE
);

-- 节点 ------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mt_plan_node (
    plan_id                  VARCHAR(64) NOT NULL,
    node_id                  VARCHAR(64) NOT NULL,
    parent_node_id           VARCHAR(64),
    name                     VARCHAR(255) NOT NULL,
    type                     VARCHAR(64)  NOT NULL,
    assignee                 VARCHAR(64),
    order_index              INT          NOT NULL,
    expected_duration_minutes INT,
    action_type              VARCHAR(64),
    completion_threshold     INT,
    action_ref               VARCHAR(255),
    description              TEXT,
    PRIMARY KEY (plan_id, node_id),
    CONSTRAINT fk_plan_node_plan FOREIGN KEY (plan_id) REFERENCES mt_plan(plan_id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_node_parent FOREIGN KEY (plan_id, parent_node_id)
        REFERENCES mt_plan_node(plan_id, node_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mt_plan_node_parent ON mt_plan_node (plan_id, parent_node_id);
CREATE INDEX IF NOT EXISTS idx_mt_plan_node_order ON mt_plan_node (plan_id, order_index);

-- 节点执行记录 ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mt_plan_node_execution (
    plan_id        VARCHAR(64) NOT NULL,
    node_id        VARCHAR(64) NOT NULL,
    status         VARCHAR(32) NOT NULL,
    start_time     TIMESTAMPTZ,
    end_time       TIMESTAMPTZ,
    operator_id    VARCHAR(64),
    result_summary TEXT,
    execution_log  TEXT,
    PRIMARY KEY (plan_id, node_id),
    CONSTRAINT fk_plan_execution_node FOREIGN KEY (plan_id, node_id)
        REFERENCES mt_plan_node(plan_id, node_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mt_plan_node_execution_status ON mt_plan_node_execution (status);
CREATE INDEX IF NOT EXISTS idx_mt_plan_node_execution_plan_status ON mt_plan_node_execution (plan_id, status);

-- 节点附件 --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mt_plan_node_attachment (
    plan_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    file_id VARCHAR(128) NOT NULL,
    PRIMARY KEY (plan_id, node_id, file_id),
    CONSTRAINT fk_plan_attachment_node FOREIGN KEY (plan_id, node_id)
        REFERENCES mt_plan_node(plan_id, node_id) ON DELETE CASCADE
);

-- 计划活动 --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mt_plan_activity (
    plan_id       VARCHAR(64) NOT NULL,
    activity_id   VARCHAR(64) NOT NULL,
    activity_type VARCHAR(64) NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL,
    actor_id      VARCHAR(64),
    message_key   VARCHAR(255),
    reference_id  VARCHAR(64),
    attributes    JSONB        NOT NULL DEFAULT '{}'::JSONB,
    PRIMARY KEY (plan_id, activity_id),
    CONSTRAINT fk_plan_activity_plan FOREIGN KEY (plan_id) REFERENCES mt_plan(plan_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mt_plan_activity_occurred ON mt_plan_activity (plan_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_mt_plan_activity_type ON mt_plan_activity (plan_id, activity_type, occurred_at DESC);

-- 提醒策略 --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mt_plan_reminder_rule (
    plan_id       VARCHAR(64) NOT NULL,
    rule_id       VARCHAR(64) NOT NULL,
    trigger       VARCHAR(64) NOT NULL,
    offset_minutes INT        NOT NULL,
    channels      JSONB       NOT NULL DEFAULT '[]'::JSONB,
    template_id   VARCHAR(64),
    recipients    JSONB       NOT NULL DEFAULT '[]'::JSONB,
    description   TEXT,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (plan_id, rule_id),
    CONSTRAINT fk_plan_reminder_plan FOREIGN KEY (plan_id) REFERENCES mt_plan(plan_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mt_plan_reminder_active ON mt_plan_reminder_rule (plan_id, active);
CREATE INDEX IF NOT EXISTS idx_mt_plan_reminder_trigger ON mt_plan_reminder_rule (plan_id, trigger);

-- 文件元数据 ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mt_file_metadata (
    file_id      VARCHAR(128) PRIMARY KEY,
    file_name    VARCHAR(512)   NOT NULL,
    content_type VARCHAR(255),
    file_size    BIGINT         NOT NULL,
    bucket       VARCHAR(255)   NOT NULL,
    object_key   VARCHAR(512)   NOT NULL,
    biz_type     VARCHAR(128),
    biz_id       VARCHAR(128),
    uploaded_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    uploader     VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_mt_file_metadata_biz ON mt_file_metadata (biz_type, biz_id, uploaded_at DESC);
