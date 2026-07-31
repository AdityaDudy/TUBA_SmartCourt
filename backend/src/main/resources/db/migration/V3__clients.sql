-- V3: Clients

CREATE TABLE IF NOT EXISTS clients (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    code        VARCHAR(20),
    name        VARCHAR(300) NOT NULL,
    type        VARCHAR(50)  DEFAULT 'Individual',
    mobile      VARCHAR(20),
    email       VARCHAR(200),
    pan         VARCHAR(20),
    gstin       VARCHAR(20),
    aadhar      VARCHAR(20),
    address     TEXT,
    city        VARCHAR(100),
    state       VARCHAR(100),
    notes       TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'active',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_clients_tenant ON clients(tenant_id);
CREATE INDEX IF NOT EXISTS idx_clients_name   ON clients(tenant_id, LOWER(name));

-- Seed sample clients
INSERT INTO clients (tenant_id, code, name, type, mobile, email, pan, status) VALUES
('default', 'CLT001', 'Rahul Gupta', 'Individual', '9876543210', 'rahul@example.com', 'ABCPG1234H', 'active'),
('default', 'CLT002', 'Infosys Ltd.', 'Corporate', '022-12345678', 'legal@infosys.com', 'AABCI1234A', 'active'),
('default', 'CLT003', 'Ministry of Finance', 'Government', '011-23456789', 'mof@gov.in', NULL, 'active'),
('default', 'CLT004', 'Sunita Sharma', 'Individual', '9871234560', 'sunita@example.com', 'BDQPS5678J', 'active'),
('default', 'CLT005', 'TechStart Pvt Ltd', 'Corporate', '9980123456', 'legal@techstart.in', 'AACTS1234B', 'active');
