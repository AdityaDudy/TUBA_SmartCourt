-- V11: IP Whitelist & Security
CREATE TABLE IF NOT EXISTS ip_whitelist (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    ip_address  VARCHAR(100) NOT NULL,
    label       VARCHAR(200),
    blocked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ip_whitelist_tenant ON ip_whitelist(tenant_id);

-- Seed initial security whitelist/block rules matching mockup
INSERT INTO ip_whitelist (ip_address, label, blocked) VALUES
('103.21.244.0/22', 'Office LAN', FALSE),
('49.248.8.100/32', 'Sr. Adv. Home', FALSE),
('185.234.218.0/24', 'Blocked', TRUE)
ON CONFLICT DO NOTHING;
