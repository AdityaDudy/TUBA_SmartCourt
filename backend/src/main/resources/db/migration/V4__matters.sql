-- V4: Matters + Matter Timeline

CREATE TABLE IF NOT EXISTS matters (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL DEFAULT 'default',
    title           VARCHAR(500) NOT NULL,
    case_no         VARCHAR(100),
    client_id       BIGINT       REFERENCES clients(id) ON DELETE SET NULL,
    client_name     VARCHAR(300),
    court           VARCHAR(200),
    type            VARCHAR(100),
    area            VARCHAR(200),
    next_hearing    DATE,
    advocate        VARCHAR(200),
    status          VARCHAR(20)  NOT NULL DEFAULT 'Active',
    stage           VARCHAR(100),
    background      TEXT,
    filing_date     DATE,
    opposite_party  VARCHAR(300),
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_matters_tenant    ON matters(tenant_id);
CREATE INDEX IF NOT EXISTS idx_matters_client    ON matters(client_id);
CREATE INDEX IF NOT EXISTS idx_matters_status    ON matters(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_matters_hearing   ON matters(next_hearing);
CREATE INDEX IF NOT EXISTS idx_matters_title_fts ON matters USING GIN(to_tsvector('english', title));

CREATE TABLE IF NOT EXISTS matter_timeline (
    id          BIGSERIAL    PRIMARY KEY,
    matter_id   BIGINT       NOT NULL REFERENCES matters(id) ON DELETE CASCADE,
    date        DATE         NOT NULL,
    event       VARCHAR(500) NOT NULL,
    sub         VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mt_matter ON matter_timeline(matter_id, date DESC);

-- Seed sample matters
INSERT INTO matters (tenant_id, title, case_no, client_name, court, type, area, next_hearing, advocate, status, stage)
VALUES
('default', 'Rahul Gupta vs State of Delhi', 'WP/1234/2025', 'Rahul Gupta', 'Delhi High Court', 'Litigation', 'Constitutional Law', '2026-07-15', 'Adv. Amit Sharma', 'Active', 'Arguments'),
('default', 'Infosys Ltd. Tax Appeal', 'ITA/567/2025', 'Infosys Ltd.', 'ITAT Delhi', 'Tax Matter', 'Taxation', '2026-07-18', 'Adv. Priya Kapoor', 'Active', 'Part Heard'),
('default', 'Ministry of Finance — FEMA Compliance', 'FEMA/89/2025', 'Ministry of Finance', 'NCLT Mumbai', 'Compliance', 'Corporate Law', '2026-07-20', 'Adv. Amit Sharma', 'Active', 'Filing'),
('default', 'Sunita Sharma Matrimonial Case', 'HMA/456/2024', 'Sunita Sharma', 'Delhi District Court', 'Litigation', 'Family Law', '2026-07-25', 'Adv. Priya Kapoor', 'Active', 'Reply Filed'),
('default', 'TechStart IP Dispute', 'CS/234/2025', 'TechStart Pvt Ltd', 'Delhi High Court', 'IP/Trademark', 'IP/Trademark', '2026-08-01', 'Adv. Amit Sharma', 'Active', 'Notice Issued');
