-- V8: Documents

CREATE TABLE IF NOT EXISTS documents (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    name        VARCHAR(500) NOT NULL,
    doc_type    VARCHAR(100),
    matter_id   BIGINT       REFERENCES matters(id) ON DELETE SET NULL,
    client_id   BIGINT       REFERENCES clients(id) ON DELETE SET NULL,
    client_name VARCHAR(300),
    s3_key      VARCHAR(1000),
    s3_url      VARCHAR(2000),
    file_size   BIGINT,
    mime_type   VARCHAR(200),
    uploaded_by VARCHAR(200),
    tags        TEXT[],
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_docs_tenant ON documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_docs_matter ON documents(matter_id);
CREATE INDEX IF NOT EXISTS idx_docs_client ON documents(client_id);

INSERT INTO documents (tenant_id, name, doc_type, client_name, uploaded_by, created_at) VALUES
('default', 'Plaint_WP1234.pdf', 'Plaint', 'Rahul Gupta', 'Adv. Amit Sharma', NOW() - INTERVAL '5 days'),
('default', 'Tax_Appeal_ITA567.pdf', 'Tax Document', 'Infosys Ltd.', 'Adv. Priya Kapoor', NOW() - INTERVAL '3 days'),
('default', 'POA_Sunita_Sharma.pdf', 'Power of Attorney', 'Sunita Sharma', 'Ravi Mehta', NOW() - INTERVAL '1 day');
