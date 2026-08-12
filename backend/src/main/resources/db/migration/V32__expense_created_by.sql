-- V32: Track User Creator on Expenses
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS created_by_id BIGINT;
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(255);

-- Backfill existing expense records to default admin user if missing creator
UPDATE expenses SET 
    created_by_id = (SELECT id FROM users WHERE tenant_id = 'default' AND role = 'admin' ORDER BY id ASC LIMIT 1),
    created_by_name = (SELECT name FROM users WHERE tenant_id = 'default' AND role = 'admin' ORDER BY id ASC LIMIT 1)
WHERE created_by_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_expenses_created_by ON expenses(created_by_id);
