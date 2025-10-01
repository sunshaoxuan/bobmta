-- -----------------------------------------------------------------------------
-- Flyway V4 - Extended filter indexes for tenant/customer analytics
-- -----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_mt_plan_tenant_customer_start ON mt_plan (tenant_id, customer_id, planned_start_time);
CREATE INDEX IF NOT EXISTS idx_mt_plan_tenant_status_end ON mt_plan (tenant_id, status, planned_end_time);
