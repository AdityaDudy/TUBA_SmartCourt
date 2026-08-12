-- V20: Upgrades to Invoicing, Payments, and Expenses

CREATE TABLE IF NOT EXISTS invoice_line_items (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           VARCHAR(100) NOT NULL DEFAULT 'default',
    invoice_id          BIGINT       REFERENCES invoices(id) ON DELETE CASCADE,
    fee_type            VARCHAR(100),
    description         VARCHAR(500) NOT NULL,
    amount              NUMERIC(14,2) NOT NULL DEFAULT 0,
    source_reference    VARCHAR(100),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ili_invoice ON invoice_line_items(invoice_id);

CREATE TABLE IF NOT EXISTS payments (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           VARCHAR(100) NOT NULL DEFAULT 'default',
    invoice_id          BIGINT       REFERENCES invoices(id) ON DELETE CASCADE,
    amount              NUMERIC(14,2) NOT NULL DEFAULT 0,
    payment_date        DATE         NOT NULL DEFAULT CURRENT_DATE,
    mode                VARCHAR(50)  NOT NULL DEFAULT 'Cash',
    reference_no        VARCHAR(100),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_payments_invoice ON payments(invoice_id);

CREATE TABLE IF NOT EXISTS expenses (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           VARCHAR(100) NOT NULL DEFAULT 'default',
    matter_id           BIGINT       REFERENCES matters(id) ON DELETE SET NULL,
    matter_title        VARCHAR(500),
    client_id           BIGINT       REFERENCES clients(id) ON DELETE SET NULL,
    client_name         VARCHAR(300),
    created_by_id       BIGINT,
    created_by_name     VARCHAR(255),
    category            VARCHAR(100) NOT NULL,
    amount              NUMERIC(14,2) NOT NULL DEFAULT 0,
    date                DATE         NOT NULL DEFAULT CURRENT_DATE,
    billable            BOOLEAN      NOT NULL DEFAULT FALSE,
    invoiced            BOOLEAN      NOT NULL DEFAULT FALSE,
    invoice_id          BIGINT       REFERENCES invoices(id) ON DELETE SET NULL,
    receipt_path        TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_expenses_tenant ON expenses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_expenses_matter ON expenses(matter_id);
CREATE INDEX IF NOT EXISTS idx_expenses_client ON expenses(client_id);

-- Seed dynamic expenses
INSERT INTO expenses (tenant_id, category, amount, date, billable, invoiced, client_name, client_id, matter_title, matter_id) VALUES
('default', 'Court Fees', 5000.00, CURRENT_DATE - 10, true, false, 'Rahul Gupta', NULL, 'Rahul Gupta vs State of Delhi', NULL),
('default', 'Travel', 1200.00, CURRENT_DATE - 8, true, false, 'Infosys Ltd.', NULL, 'Infosys Ltd. Tax Appeal', NULL),
('default', 'Stamp Duty', 8500.00, CURRENT_DATE - 5, false, false, 'TechStart Pvt Ltd', NULL, 'TechStart IP Dispute', NULL);
