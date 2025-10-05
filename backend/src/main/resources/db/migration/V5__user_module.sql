-- -----------------------------------------------------------------------------
-- User & Identity tables
-- -----------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS mt_user_id_seq AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS mt_user (
    user_id       VARCHAR(64) PRIMARY KEY,
    username      VARCHAR(128)  NOT NULL,
    display_name  VARCHAR(128)  NOT NULL,
    email         VARCHAR(256)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    status        VARCHAR(32)   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_mt_user_username_ci ON mt_user ((LOWER(username)));
CREATE UNIQUE INDEX IF NOT EXISTS uq_mt_user_email_ci ON mt_user ((LOWER(email)));

CREATE TABLE IF NOT EXISTS mt_user_role (
    user_id VARCHAR(64) NOT NULL,
    role    VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES mt_user(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mt_user_role_role ON mt_user_role (role);

CREATE TABLE IF NOT EXISTS mt_user_activation_token (
    user_id    VARCHAR(64)  PRIMARY KEY,
    token      VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_activation_user FOREIGN KEY (user_id) REFERENCES mt_user(user_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_mt_user_activation_token ON mt_user_activation_token (token);
