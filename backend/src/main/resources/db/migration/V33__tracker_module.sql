-- V33: CNR Case Tracker Module
-- Tables: tracked_cases, case_hearings, case_orders, case_alert_subscriptions, scrape_jobs

-- ── Tracked Cases (one row per CNR, updated on each sync) ──────────────────────────────────
CREATE TABLE IF NOT EXISTS tracked_cases (
    id                   BIGSERIAL    PRIMARY KEY,
    tenant_id            VARCHAR(100) NOT NULL DEFAULT 'default',
    cnr                  VARCHAR(20)  NOT NULL,
    case_type            VARCHAR(200),
    filing_no            VARCHAR(100),
    filing_date          DATE,
    registration_no      VARCHAR(100),
    registration_date    DATE,
    court_name           VARCHAR(500),
    court_complex        VARCHAR(500),
    judge_name           VARCHAR(500),
    case_status          VARCHAR(50),          -- PENDING, DISPOSED, STAYED, DISMISSED, TRANSFERRED
    stage_of_case        VARCHAR(200),
    next_hearing_date    DATE,
    petitioners          TEXT[],
    respondents          TEXT[],
    petitioner_advocates TEXT[],
    respondent_advocates TEXT[],
    acts_and_sections    TEXT[],
    fir_no               VARCHAR(100),
    fir_year             VARCHAR(10),
    police_station       VARCHAR(300),
    matter_id            BIGINT        REFERENCES matters(id) ON DELETE SET NULL,
    last_fetched_at      TIMESTAMP WITH TIME ZONE,
    snapshot_version     INTEGER       NOT NULL DEFAULT 0,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_tracked_case_cnr_tenant UNIQUE (cnr, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_tc_tenant         ON tracked_cases(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tc_cnr            ON tracked_cases(cnr);
CREATE INDEX IF NOT EXISTS idx_tc_last_fetched   ON tracked_cases(last_fetched_at);
CREATE INDEX IF NOT EXISTS idx_tc_matter         ON tracked_cases(matter_id);

-- ── Case Hearings (one row per hearing date, chronological) ────────────────────────────────
CREATE TABLE IF NOT EXISTS case_hearings (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           VARCHAR(100) NOT NULL DEFAULT 'default',
    tracked_case_id     BIGINT       NOT NULL REFERENCES tracked_cases(id) ON DELETE CASCADE,
    hearing_date        DATE,
    judge               VARCHAR(500),
    purpose_of_hearing  VARCHAR(500),
    next_hearing_date   DATE,
    business_remarks    TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ch_case   ON case_hearings(tracked_case_id);
CREATE INDEX IF NOT EXISTS idx_ch_date   ON case_hearings(hearing_date);

-- ── Case Orders / Judgments (with S3 storage) ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS case_orders (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        VARCHAR(100) NOT NULL DEFAULT 'default',
    tracked_case_id  BIGINT       NOT NULL REFERENCES tracked_cases(id) ON DELETE CASCADE,
    order_date       DATE,
    order_no         VARCHAR(100),
    order_type       VARCHAR(100),             -- interim, final, etc.
    s3_key           VARCHAR(1000),
    s3_url           VARCHAR(2000),
    file_size        BIGINT,
    mime_type        VARCHAR(200),
    content_hash     VARCHAR(64),              -- SHA-256 hex, for dedup
    external_url     VARCHAR(2000),            -- original eCourts URL (for traceability)
    downloaded_at    TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_co_case   ON case_orders(tracked_case_id);
CREATE INDEX IF NOT EXISTS idx_co_hash   ON case_orders(content_hash);

-- ── Case Alert Subscriptions ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS case_alert_subscriptions (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        VARCHAR(100) NOT NULL DEFAULT 'default',
    tracked_case_id  BIGINT       NOT NULL REFERENCES tracked_cases(id) ON DELETE CASCADE,
    user_id          BIGINT       NOT NULL,
    channels         TEXT[]       NOT NULL DEFAULT '{"in-app"}',
    active           BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_alert_case_user UNIQUE (tracked_case_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_cas_user   ON case_alert_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_cas_active ON case_alert_subscriptions(active);

-- ── Scrape Jobs (DB-backed async queue) ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS scrape_jobs (
    id                    BIGSERIAL    PRIMARY KEY,
    tenant_id             VARCHAR(100) NOT NULL DEFAULT 'default',
    cnr                   VARCHAR(20)  NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',   -- PENDING, RUNNING, DONE, FAILED, CAPTCHA_REQUIRED
    initiated_by_user_id  BIGINT,
    force_refresh         BOOLEAN      NOT NULL DEFAULT false,
    result_json           TEXT,
    captcha_image_s3_url  VARCHAR(2000),
    error_message         TEXT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    started_at            TIMESTAMP WITH TIME ZONE,
    completed_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_sj_status     ON scrape_jobs(status);
CREATE INDEX IF NOT EXISTS idx_sj_cnr        ON scrape_jobs(cnr);
CREATE INDEX IF NOT EXISTS idx_sj_user       ON scrape_jobs(initiated_by_user_id);
CREATE INDEX IF NOT EXISTS idx_sj_created    ON scrape_jobs(created_at DESC);
