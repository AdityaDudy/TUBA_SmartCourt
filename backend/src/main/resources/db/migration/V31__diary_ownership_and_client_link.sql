-- V31: Diary Ownership and Client Link
ALTER TABLE diary_events ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE diary_events ADD COLUMN IF NOT EXISTS owner_name VARCHAR(255);
ALTER TABLE diary_events ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE diary_events ADD COLUMN IF NOT EXISTS client_id BIGINT;
ALTER TABLE diary_events ADD COLUMN IF NOT EXISTS client_name VARCHAR(255);

-- Backfill existing rows to default admin user if unowned
UPDATE diary_events SET 
    owner_id = (SELECT id FROM users WHERE tenant_id = 'default' AND role = 'admin' ORDER BY id ASC LIMIT 1),
    owner_name = (SELECT name FROM users WHERE tenant_id = 'default' AND role = 'admin' ORDER BY id ASC LIMIT 1)
WHERE owner_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_diary_events_owner ON diary_events(owner_id);
CREATE INDEX IF NOT EXISTS idx_diary_events_tenant_date ON diary_events(tenant_id, event_date);
