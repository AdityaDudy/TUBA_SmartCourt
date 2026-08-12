-- V2: Masters — configurable lookup data per tenant (courts, types, etc.)

CREATE TABLE IF NOT EXISTS masters (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    category    VARCHAR(100) NOT NULL,
    items       TEXT[]       NOT NULL DEFAULT '{}',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_master_tenant_category UNIQUE (tenant_id, category)
);

CREATE INDEX IF NOT EXISTS idx_masters_tenant ON masters(tenant_id);

-- Seed default master data
INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'courts', ARRAY[
    'Supreme Court of India',
    'Delhi High Court',
    'Bombay High Court',
    'Madras High Court',
    'Calcutta High Court',
    'NCLT Mumbai',
    'NCLT Delhi',
    'ITAT Delhi',
    'ITAT Mumbai',
    'RERA Maharashtra',
    'RERA Delhi',
    'SAT Mumbai',
    'NCLAT Delhi',
    'Income Tax Appellate Tribunal',
    'Consumer Forum District',
    'State Consumer Forum',
    'National Consumer Commission',
    'Delhi District Court',
    'Mumbai City Civil Court',
    'Sessions Court Delhi'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'matterTypes', ARRAY[
    'Litigation',
    'Arbitration',
    'Advisory',
    'IBC/NCLT',
    'Compliance',
    'Tax Matter',
    'Consumer',
    'Real Estate',
    'Criminal',
    'Constitutional',
    'Labour',
    'IP/Trademark',
    'Mergers & Acquisitions',
    'Contract Review'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'practiceAreas', ARRAY[
    'Corporate Law',
    'Litigation',
    'Arbitration',
    'Insolvency & Bankruptcy',
    'Taxation',
    'Real Estate',
    'Labour & Employment',
    'Intellectual Property',
    'Criminal Defense',
    'Constitutional Law',
    'Consumer Protection',
    'Banking & Finance',
    'Mergers & Acquisitions',
    'Environmental Law'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'stages', ARRAY[
    'Filing',
    'Admission',
    'Notice Issued',
    'Reply Filed',
    'Arguments',
    'Part Heard',
    'Judgment Reserved',
    'Disposed',
    'Appeal Filed',
    'Execution'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'taskTypes', ARRAY[
    'Court Appearance',
    'Document Drafting',
    'Research',
    'Client Meeting',
    'Filing',
    'Review',
    'Follow-up',
    'Other'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'priorities', ARRAY[
    'Urgent',
    'High',
    'Medium',
    'Low'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'docTypes', ARRAY[
    'Plaint',
    'Written Statement',
    'Affidavit',
    'Court Order',
    'Judgment',
    'Notice',
    'Agreement',
    'Power of Attorney',
    'Evidence',
    'Vakalat Nama',
    'Memo of Appearance',
    'Application'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'filingStages', ARRAY[
    'Draft',
    'Ready to File',
    'Filed',
    'Defective',
    'Re-filed',
    'Numbered'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'clientTypes', ARRAY[
    'Individual',
    'Corporate',
    'Government',
    'NGO/Trust',
    'HUF',
    'Partnership'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'designations', ARRAY[
    'Senior Partner',
    'Partner',
    'Senior Advocate',
    'Advocate',
    'Junior Advocate',
    'Legal Clerk',
    'Paralegal',
    'Intern'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'departments', ARRAY[
    'Litigation',
    'Corporate',
    'Tax',
    'IP',
    'Labour',
    'Real Estate',
    'Criminal',
    'Administration'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

INSERT INTO masters (tenant_id, category, items) VALUES
('default', 'roles', ARRAY[
    'admin',
    'senior',
    'advocate',
    'clerk',
    'readonly'
]) ON CONFLICT (tenant_id, category) DO NOTHING;

