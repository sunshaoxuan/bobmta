-- -----------------------------------------------------------------------------
-- Flyway V2 - Additional indexes to support analytics and reminders queries
-- -----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_mt_plan_tenant_owner_end ON mt_plan (tenant_id, owner_id, planned_end_time);
CREATE INDEX IF NOT EXISTS idx_mt_plan_status_end ON mt_plan (status, planned_end_time);
CREATE INDEX IF NOT EXISTS idx_mt_plan_node_execution_plan_status ON mt_plan_node_execution (plan_id, status);
CREATE INDEX IF NOT EXISTS idx_mt_plan_activity_type ON mt_plan_activity (plan_id, activity_type, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_mt_plan_reminder_trigger ON mt_plan_reminder_rule (plan_id, trigger);
