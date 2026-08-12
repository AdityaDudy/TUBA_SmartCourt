-- V7: Filings

CREATE TABLE IF NOT EXISTS filings (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    title       VARCHAR(500) NOT NULL,
    matter_id   BIGINT       REFERENCES matters(id) ON DELETE SET NULL,
    matter_title VARCHAR(500),
    court       VARCHAR(200),
    filing_type VARCHAR(100),
    stage       VARCHAR(100) DEFAULT 'Draft',
    status      VARCHAR(20)  NOT NULL DEFAULT 'Draft',
    due_date    DATE,
    filed_date  DATE,
    advocate    VARCHAR(200),
    notes       TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_filings_tenant ON filings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_filings_status ON filings(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_filings_matter ON filings(matter_id);

INSERT INTO filings (tenant_id, title, matter_title, court, filing_type, stage, status, due_date, advocate) VALUES
('default', 'Rejoinder in WP/1234/2025', 'Rahul Gupta vs State of Delhi', 'Delhi High Court', 'Rejoinder', 'Draft', 'Draft', CURRENT_DATE, 'Adv. Amit Sharma'),
('default', 'Reply to Show Cause Notice', 'Ministry of Finance — FEMA Compliance', 'NCLT Mumbai', 'Reply', 'Ready to File', 'Ready to File', CURRENT_DATE + 1, 'Adv. Priya Kapoor'),
('default', 'SLP against HC Order', 'Rahul Gupta vs State of Delhi', 'Supreme Court of India', 'SLP', 'Filed', 'Filed', CURRENT_DATE - 5, 'Adv. Amit Sharma');
