-- V38__pagination_indexes.sql
-- Composite indexes to support JpaSpecificationExecutor-based paginated queries.
-- Every index follows the pattern: (tenant_id, filter_column [, sort_column]).

-- ── matters ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_matters_tenant_status
    ON matters (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_matters_tenant_type
    ON matters (tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_matters_tenant_next_hearing
    ON matters (tenant_id, next_hearing);

CREATE INDEX IF NOT EXISTS idx_matters_tenant_advocate
    ON matters (tenant_id, advocate);

-- ── tasks ─────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_tasks_tenant_done
    ON tasks (tenant_id, done);

CREATE INDEX IF NOT EXISTS idx_tasks_tenant_status
    ON tasks (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_tasks_tenant_assigned_to
    ON tasks (tenant_id, assigned_to);

-- ── invoices ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status
    ON invoices (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_invoices_tenant_client_id
    ON invoices (tenant_id, client_id);

CREATE INDEX IF NOT EXISTS idx_invoices_tenant_matter_id
    ON invoices (tenant_id, matter_id);

-- ── filings ───────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_filings_tenant_status
    ON filings (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_filings_tenant_stage
    ON filings (tenant_id, stage);

CREATE INDEX IF NOT EXISTS idx_filings_tenant_advocate
    ON filings (tenant_id, advocate);

-- ── clients ───────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_clients_tenant_type
    ON clients (tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_clients_tenant_status
    ON clients (tenant_id, status);

-- ── audit_log ─────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_created_at
    ON audit_log (tenant_id, created_at DESC);

-- ── hearings ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_hearings_tenant_hearing_date
    ON hearings (tenant_id, hearing_date);

CREATE INDEX IF NOT EXISTS idx_hearings_tenant_status
    ON hearings (tenant_id, status);

-- ── invoice_line_items ────────────────────────────────────────────────────────
-- Supports the tenant-scoped lookup in getPendingBillables()
CREATE INDEX IF NOT EXISTS idx_invoice_line_items_tenant
    ON invoice_line_items (tenant_id);
