-- -----------------------------------------------------------------------------
-- Flyway V7 - Plan action history persistence
-- -----------------------------------------------------------------------------

CREATE TABLE mt_plan_action_history (
    action_id      VARCHAR(80)  PRIMARY KEY,
    plan_id        VARCHAR(64)  NOT NULL,
    node_id        VARCHAR(64)  NOT NULL,
    action_type    VARCHAR(64)  NOT NULL,
    action_ref     VARCHAR(255),
    triggered_at   TIMESTAMPTZ  NOT NULL,
    triggered_by   VARCHAR(64),
    status         VARCHAR(32)  NOT NULL,
    message_key    VARCHAR(255),
    error_message  TEXT,
    context        JSONB        NOT NULL DEFAULT '{}'::JSONB,
    metadata       JSONB        NOT NULL DEFAULT '{}'::JSONB,
    CONSTRAINT fk_plan_action_history_plan FOREIGN KEY (plan_id)
        REFERENCES mt_plan(plan_id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_action_history_node FOREIGN KEY (plan_id, node_id)
        REFERENCES mt_plan_node(plan_id, node_id) ON DELETE CASCADE
);

CREATE INDEX idx_mt_plan_action_plan ON mt_plan_action_history (plan_id, triggered_at DESC);
CREATE INDEX idx_mt_plan_action_plan_node ON mt_plan_action_history (plan_id, node_id, triggered_at DESC);

