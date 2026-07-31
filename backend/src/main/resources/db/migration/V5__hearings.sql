-- V5: Hearings / Cause List

CREATE TABLE IF NOT EXISTS hearings (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    matter_id   BIGINT       REFERENCES matters(id) ON DELETE SET NULL,
    case_title  VARCHAR(500) NOT NULL,
    case_no     VARCHAR(100),
    court       VARCHAR(200),
    bench       VARCHAR(200),
    hearing_date DATE        NOT NULL,
    hearing_time VARCHAR(10),
    stage       VARCHAR(100),
    advocate    VARCHAR(200),
    status      VARCHAR(20)  NOT NULL DEFAULT 'Scheduled',
    result      VARCHAR(200),
    next_date   DATE,
    notes       TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_hearings_tenant ON hearings(tenant_id, hearing_date);
CREATE INDEX IF NOT EXISTS idx_hearings_date   ON hearings(hearing_date);
CREATE INDEX IF NOT EXISTS idx_hearings_matter ON hearings(matter_id);

-- Seed today's and upcoming hearings
INSERT INTO hearings (tenant_id, case_title, case_no, court, bench, hearing_date, hearing_time, stage, advocate, status)
VALUES
('default', 'Rahul Gupta vs State of Delhi', 'WP/1234/2025', 'Delhi High Court', 'Bench IV', CURRENT_DATE, '10:30', 'Arguments', 'Adv. Amit Sharma', 'Scheduled'),
('default', 'Infosys Ltd. Tax Appeal', 'ITA/567/2025', 'ITAT Delhi', 'Bench A', CURRENT_DATE, '11:00', 'Part Heard', 'Adv. Priya Kapoor', 'Urgent'),
('default', 'Ministry of Finance — FEMA Compliance', 'FEMA/89/2025', 'NCLT Mumbai', 'Bench 5', CURRENT_DATE, '14:00', 'Filing', 'Adv. Amit Sharma', 'Urgent'),
('default', 'Sunita Sharma Matrimonial Case', 'HMA/456/2024', 'Delhi District Court', 'Family Court 3', CURRENT_DATE, '11:30', 'Reply Filed', 'Adv. Priya Kapoor', 'Scheduled'),
('default', 'TechStart IP Dispute', 'CS/234/2025', 'Delhi High Court', 'Bench II', CURRENT_DATE, '15:15', 'Notice Issued', 'Adv. Amit Sharma', 'Scheduled');
