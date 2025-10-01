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
