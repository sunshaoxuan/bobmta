-- -----------------------------------------------------------------------------
-- Flyway V5 - Customer, Tag, Custom Field, and Audit modules
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS mt_multilingual_texts (
    scope          VARCHAR(64)  NOT NULL,
    entity_id      VARCHAR(128) NOT NULL,
    field          VARCHAR(64)  NOT NULL,
    default_locale VARCHAR(32)  NOT NULL,
    translations   JSONB        NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (scope, entity_id, field)
);

CREATE TABLE mt_customer (
    customer_id VARCHAR(64) PRIMARY KEY,
    tenant_id   VARCHAR(64)  NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    short_name  VARCHAR(255),
    group_name  VARCHAR(255),
    region      VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mt_customer_tenant_code ON mt_customer (tenant_id, code);
CREATE INDEX idx_mt_customer_region ON mt_customer (region);

CREATE TABLE mt_tag_definition (
    tag_id         BIGSERIAL    PRIMARY KEY,
    tenant_id      VARCHAR(64)  NOT NULL,
    default_locale VARCHAR(32)  NOT NULL,
    default_name   VARCHAR(255) NOT NULL,
    color          VARCHAR(32),
    icon           VARCHAR(64),
    scope          VARCHAR(16)  NOT NULL,
    apply_rule     TEXT,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mt_tag_definition_tenant ON mt_tag_definition (tenant_id, scope);

CREATE TABLE mt_tag_assignment (
    tag_id     BIGINT       NOT NULL,
    tenant_id  VARCHAR(64)  NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id  VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, tag_id, entity_type, entity_id),
    CONSTRAINT fk_tag_assignment_definition FOREIGN KEY (tag_id)
        REFERENCES mt_tag_definition (tag_id) ON DELETE CASCADE
);

CREATE TABLE mt_custom_field_definition (
    field_id    BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(64)  NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    label       VARCHAR(255) NOT NULL,
    type        VARCHAR(16)  NOT NULL,
    required    BOOLEAN      NOT NULL DEFAULT FALSE,
    options     JSONB        NOT NULL DEFAULT '[]'::JSONB,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_mt_custom_field_definition_code
    ON mt_custom_field_definition (tenant_id, lower(code));

CREATE TABLE mt_custom_field_value (
    field_id   BIGINT      NOT NULL,
    tenant_id  VARCHAR(64) NOT NULL,
    entity_id  VARCHAR(128) NOT NULL,
    value      TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, field_id, entity_id),
    CONSTRAINT fk_custom_field_value_definition FOREIGN KEY (field_id)
        REFERENCES mt_custom_field_definition (field_id) ON DELETE CASCADE
);

CREATE INDEX idx_mt_custom_field_value_entity ON mt_custom_field_value (tenant_id, entity_id);

CREATE TABLE mt_audit_log (
    audit_id   BIGSERIAL    PRIMARY KEY,
    tenant_id  VARCHAR(64)  NOT NULL,
    timestamp  TIMESTAMPTZ  NOT NULL,
    user_id    VARCHAR(64),
    username   VARCHAR(128),
    entity_type VARCHAR(64),
    entity_id  VARCHAR(128),
    action     VARCHAR(128),
    detail     TEXT,
    old_data   TEXT,
    new_data   TEXT,
    request_id VARCHAR(128),
    ip_address VARCHAR(64),
    user_agent VARCHAR(255)
);

CREATE INDEX idx_mt_audit_log_tenant_time ON mt_audit_log (tenant_id, timestamp DESC);
CREATE INDEX idx_mt_audit_log_entity ON mt_audit_log (tenant_id, entity_type, entity_id);

-- Seed default tenant data
INSERT INTO mt_customer (customer_id, tenant_id, code, name, short_name, group_name, region, created_at, updated_at)
VALUES
    ('101', 'tenant-001', 'CUST-101', '北海道大学', '北大', '教育', '北海道', NOW(), NOW()),
    ('102', 'tenant-001', 'CUST-102', '北見工業大学', '北見工大', '教育', '北海道', NOW(), NOW()),
    ('201', 'tenant-001', 'CUST-201', '東京メトロ', '東京メトロ', '交通', '関東', NOW(), NOW());

INSERT INTO mt_tag_definition (tenant_id, default_locale, default_name, color, icon, scope, apply_rule, enabled)
VALUES
    ('tenant-001', 'ja-JP', '重点対応', '#FF5722', 'StarOutlined', 'CUSTOMER', NULL, TRUE),
    ('tenant-001', 'ja-JP', '年間契約', '#1890FF', 'CalendarOutlined', 'PLAN', NULL, TRUE);

-- link tags to customers
INSERT INTO mt_tag_assignment (tag_id, tenant_id, entity_type, entity_id)
VALUES
    (1, 'tenant-001', 'CUSTOMER', '101'),
    (1, 'tenant-001', 'CUSTOMER', '102');

INSERT INTO mt_custom_field_definition (tenant_id, code, label, type, required, options, description)
VALUES
    ('tenant-001', 'erp_version', 'ERPバージョン', 'TEXT', TRUE, '[]'::JSONB, '顧客のERPシステムバージョン'),
    ('tenant-001', 'critical_system', '中核システム', 'TEXT', FALSE, '[]'::JSONB, '重要システム名');

INSERT INTO mt_custom_field_value (field_id, tenant_id, entity_id, value)
VALUES
    (1, 'tenant-001', '101', '2024.1'),
    (2, 'tenant-001', '101', '大学基幹ネットワーク'),
    (1, 'tenant-001', '102', '2023.4');

-- Seed multilingual texts for tags
INSERT INTO mt_multilingual_texts (scope, entity_id, field, default_locale, translations, updated_at)
VALUES
    ('TAG_DEFINITION', '1', 'name', 'ja-jp', '{"ja-jp": "重点対応", "zh-cn": "重点对接"}', NOW()),
    ('TAG_DEFINITION', '2', 'name', 'ja-jp', '{"ja-jp": "年間契約", "zh-cn": "年度合同"}', NOW())
ON CONFLICT (scope, entity_id, field) DO NOTHING;
