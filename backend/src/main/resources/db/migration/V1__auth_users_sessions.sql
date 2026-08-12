-- V1: Auth — users, refresh tokens, sessions, audit log
-- PostgreSQL 18 compatible

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100)  NOT NULL DEFAULT 'default',
    name            VARCHAR(200)  NOT NULL,
    email           VARCHAR(200)  NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,
    role            VARCHAR(50)   NOT NULL DEFAULT 'advocate',
    department      VARCHAR(100),
    designation     VARCHAR(100),
    mobile          VARCHAR(20),
    bar_council_no  VARCHAR(100),
    mfa_secret      VARCHAR(255),
    mfa_enabled     BOOLEAN       NOT NULL DEFAULT FALSE,
    status          VARCHAR(20)   NOT NULL DEFAULT 'active',
    initials        VARCHAR(5),
    gradient        VARCHAR(200),
    permissions     TEXT[],
    failed_attempts INT           NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP WITH TIME ZONE,
    last_login      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_user_email_tenant UNIQUE (email, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_users_email     ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rt_token_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_rt_user_id    ON refresh_tokens(user_id);

CREATE TABLE IF NOT EXISTS user_sessions (
    id          VARCHAR(36)  PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id   VARCHAR(100) NOT NULL,
    ip_address  VARCHAR(50),
    user_agent  VARCHAR(500),
    device      VARCHAR(200),
    location    VARCHAR(200),
    last_active TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON user_sessions(user_id);

CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    user_id     BIGINT,
    user_email  VARCHAR(200),
    action      VARCHAR(100) NOT NULL,
    entity      VARCHAR(100),
    entity_id   VARCHAR(100),
    details     TEXT,
    ip_address  VARCHAR(50),
    risk        VARCHAR(20)  DEFAULT 'LOW',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_tenant  ON audit_log(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_user    ON audit_log(user_id);

-- Insert a default admin user (password: Password@123)
INSERT INTO users (name, email, password_hash, role, department, designation, initials, gradient, mfa_enabled, status, permissions)
VALUES (
    'Adv. Amit Sharma',
    'amit@tubalaw.com',
    '$2a$12$LqXkr7c8T2j9RmNqWvPdE.O3jQkOL0x7Yb6NvT8UeJcMxS9R2lVa',
    'admin',
    'Litigation',
    'Senior Partner',
    'AS',
    'linear-gradient(135deg,#b45309,#d97706)',
    FALSE,
    'active',
    ARRAY['view_all','create_matters','edit_matters','delete_matters','view_docs','upload_docs',
          'delete_docs','manage_tasks','view_billing','create_invoices','manage_clients',
          'export_data','manage_users','system_settings','view_audit']
) ON CONFLICT (email, tenant_id) DO NOTHING;

INSERT INTO users (name, email, password_hash, role, department, designation, initials, gradient, mfa_enabled, status, permissions)
VALUES (
    'Adv. Priya Kapoor',
    'priya@tubalaw.com',
    '$2a$12$LqXkr7c8T2j9RmNqWvPdE.O3jQkOL0x7Yb6NvT8UeJcMxS9R2lVa',
    'senior',
    'Corporate',
    'Senior Advocate',
    'PK',
    'linear-gradient(135deg,#0d6637,#16a34a)',
    FALSE,
    'active',
    ARRAY['view_all','create_matters','edit_matters','view_docs','upload_docs',
          'manage_tasks','view_billing','manage_clients','export_data']
) ON CONFLICT (email, tenant_id) DO NOTHING;

INSERT INTO users (name, email, password_hash, role, department, designation, initials, gradient, mfa_enabled, status, permissions)
VALUES (
    'Ravi Mehta',
    'ravi@tubalaw.com',
    '$2a$12$LqXkr7c8T2j9RmNqWvPdE.O3jQkOL0x7Yb6NvT8UeJcMxS9R2lVa',
    'clerk',
    'Filing',
    'Legal Clerk',
    'RM',
    'linear-gradient(135deg,#0f766e,#0d9488)',
    FALSE,
    'active',
    ARRAY['view_all','view_docs','upload_docs','manage_tasks','view_billing']
) ON CONFLICT (email, tenant_id) DO NOTHING;
