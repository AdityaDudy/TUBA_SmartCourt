-- V9: Billing — Invoices + Billing Entries

CREATE TABLE IF NOT EXISTS invoices (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL DEFAULT 'default',
    invoice_no      VARCHAR(50),
    client_id       BIGINT       REFERENCES clients(id) ON DELETE SET NULL,
    client_name     VARCHAR(300),
    matter_id       BIGINT       REFERENCES matters(id) ON DELETE SET NULL,
    matter_title    VARCHAR(500),
    amount          NUMERIC(14,2) NOT NULL DEFAULT 0,
    paid_amount     NUMERIC(14,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'Draft',
    due_date        DATE,
    paid_date       DATE,
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoices_client ON invoices(client_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(tenant_id, status);

CREATE TABLE IF NOT EXISTS billing_entries (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    invoice_id  BIGINT       REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    hours       NUMERIC(6,2),
    rate        NUMERIC(12,2),
    amount      NUMERIC(14,2) NOT NULL DEFAULT 0,
    date        DATE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_be_invoice ON billing_entries(invoice_id);

INSERT INTO invoices (tenant_id, invoice_no, client_name, matter_title, amount, paid_amount, status, due_date) VALUES
('default', 'INV-2026-001', 'Rahul Gupta', 'Rahul Gupta vs State of Delhi', 50000, 50000, 'Paid', CURRENT_DATE - 30),
('default', 'INV-2026-002', 'Infosys Ltd.', 'Infosys Ltd. Tax Appeal', 150000, 0, 'Overdue', CURRENT_DATE - 15),
('default', 'INV-2026-003', 'TechStart Pvt Ltd', 'TechStart IP Dispute', 75000, 0, 'Sent', CURRENT_DATE + 15),
('default', 'INV-2026-004', 'Sunita Sharma', 'Sunita Sharma Matrimonial Case', 30000, 30000, 'Paid', CURRENT_DATE - 10);
