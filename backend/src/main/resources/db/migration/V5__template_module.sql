CREATE TABLE IF NOT EXISTS mt_template_definition (
    template_id       BIGSERIAL PRIMARY KEY,
    template_type     VARCHAR(20) NOT NULL,
    to_recipients     JSONB       NOT NULL DEFAULT '[]'::JSONB,
    cc_recipients     JSONB       NOT NULL DEFAULT '[]'::JSONB,
    endpoint          TEXT,
    enabled           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mt_template_definition_type
    ON mt_template_definition (template_type);
