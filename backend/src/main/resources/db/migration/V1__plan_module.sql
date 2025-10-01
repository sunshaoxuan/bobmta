-- -----------------------------------------------------------------------------
-- Flyway V1 - Plan module schema baseline
-- -----------------------------------------------------------------------------

CREATE SEQUENCE mt_plan_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE mt_plan_node_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE mt_plan_reminder_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE mt_plan (
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

CREATE INDEX idx_mt_plan_tenant_status ON mt_plan (tenant_id, status);
CREATE INDEX idx_mt_plan_tenant_start ON mt_plan (tenant_id, planned_start_time);
CREATE INDEX idx_mt_plan_tenant_end ON mt_plan (tenant_id, planned_end_time);
CREATE INDEX idx_mt_plan_customer_status ON mt_plan (customer_id, status);
CREATE INDEX idx_mt_plan_owner_status ON mt_plan (owner_id, status);

CREATE TABLE mt_plan_participant (
    plan_id        VARCHAR(64) NOT NULL,
    participant_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (plan_id, participant_id),
    CONSTRAINT fk_plan_participant_plan FOREIGN KEY (plan_id) REFERENCES mt_plan(plan_id) ON DELETE CASCADE
);

CREATE TABLE mt_plan_node (
    plan_id                   VARCHAR(64) NOT NULL,
    node_id                   VARCHAR(64) NOT NULL,
    parent_node_id            VARCHAR(64),
    name                      VARCHAR(255) NOT NULL,
    type                      VARCHAR(64)  NOT NULL,
    assignee                  VARCHAR(64),
    order_index               INT          NOT NULL,
    expected_duration_minutes INT,
    action_type               VARCHAR(64),
    completion_threshold      INT,
    action_ref                VARCHAR(255),
    description               TEXT,
    PRIMARY KEY (plan_id, node_id),
    CONSTRAINT fk_plan_node_plan FOREIGN KEY (plan_id) REFERENCES mt_plan(plan_id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_node_parent FOREIGN KEY (plan_id, parent_node_id)
        REFERENCES mt_plan_node(plan_id, node_id) ON DELETE CASCADE
);

CREATE INDEX idx_mt_plan_node_parent ON mt_plan_node (plan_id, parent_node_id);
CREATE INDEX idx_mt_plan_node_order ON mt_plan_node (plan_id, order_index);

CREATE TABLE mt_plan_node_execution (
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

CREATE INDEX idx_mt_plan_node_execution_status ON mt_plan_node_execution (status);

CREATE TABLE mt_plan_node_attachment (
    plan_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    file_id VARCHAR(128) NOT NULL,
    PRIMARY KEY (plan_id, node_id, file_id),
    CONSTRAINT fk_plan_attachment_node FOREIGN KEY (plan_id, node_id)
        REFERENCES mt_plan_node(plan_id, node_id) ON DELETE CASCADE
);

CREATE TABLE mt_plan_activity (
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

CREATE INDEX idx_mt_plan_activity_occurred ON mt_plan_activity (plan_id, occurred_at DESC);

CREATE TABLE mt_plan_reminder_rule (
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

CREATE INDEX idx_mt_plan_reminder_active ON mt_plan_reminder_rule (plan_id, active);
