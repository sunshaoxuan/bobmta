ALTER TABLE mt_template_definition
    ADD COLUMN IF NOT EXISTS name_default_locale VARCHAR(32),
    ADD COLUMN IF NOT EXISTS name_translations JSONB NOT NULL DEFAULT '{}'::JSONB,
    ADD COLUMN IF NOT EXISTS subject_default_locale VARCHAR(32),
    ADD COLUMN IF NOT EXISTS subject_translations JSONB NOT NULL DEFAULT '{}'::JSONB,
    ADD COLUMN IF NOT EXISTS content_default_locale VARCHAR(32),
    ADD COLUMN IF NOT EXISTS content_translations JSONB NOT NULL DEFAULT '{}'::JSONB,
    ADD COLUMN IF NOT EXISTS description_default_locale VARCHAR(32),
    ADD COLUMN IF NOT EXISTS description_translations JSONB NOT NULL DEFAULT '{}'::JSONB;
