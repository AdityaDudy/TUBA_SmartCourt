-- V36: Cause List ← eCourts sync support
--
-- 1. Unique constraint (tenant_id, matter_id, hearing_date) for matter-linked rows.
--    This enforces the upsert key used by CauseListSyncService so repeated syncs
--    can never produce duplicate Hearing rows for the same matter + date.
--    Partial: only applies where matter_id IS NOT NULL (manually created hearings
--    without a matter link are unaffected).
--
-- 2. One-time bulk status flip — any existing hearings whose date is in the past
--    get their status set to 'Completed' so they don't pollute the upcoming view.

CREATE UNIQUE INDEX IF NOT EXISTS idx_hearings_matter_date_uq
    ON hearings (tenant_id, matter_id, hearing_date)
    WHERE matter_id IS NOT NULL;

UPDATE hearings
    SET status = 'Completed'
    WHERE hearing_date < CURRENT_DATE
      AND status <> 'Completed';
