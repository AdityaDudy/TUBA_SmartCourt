-- V37: Add new permission tokens for Court Tracker, Filings, and Audit Log access
-- New permissions:
--   use_court_tracker  — access the eCourts tracker (search, download, export)
--   manage_filings     — create/update/delete filings
--   view_org_audit     — view org-wide audit log
--   edit_docs          — aliased as upload_docs; unify naming
--   delete_clients     — already existed but ensure consistency

-- Update role permission defaults to include new tokens
UPDATE system_settings
SET setting_value = 'view_all,create_matters,edit_matters,delete_matters,view_docs,upload_docs,edit_docs,delete_docs,manage_tasks,manage_tasks_assign,manage_tasks_close,view_billing,view_own_billing,create_invoices,export_billing,export_data,manage_clients,delete_clients,manage_users,manage_roles,impersonate_user,system_settings,view_audit,view_own_audit,view_org_audit,scope_org,use_court_tracker,manage_filings'
WHERE setting_key = 'role.permissions.admin';

UPDATE system_settings
SET setting_value = 'view_all,create_matters,edit_matters,view_docs,upload_docs,edit_docs,manage_tasks,manage_tasks_assign,manage_tasks_close,view_billing,export_billing,manage_clients,export_data,scope_team,use_court_tracker,manage_filings,view_org_audit'
WHERE setting_key = 'role.permissions.senior';

UPDATE system_settings
SET setting_value = 'view_all,create_matters,edit_matters,view_docs,upload_docs,edit_docs,manage_tasks_close,view_own_billing,scope_own,use_court_tracker,manage_filings,view_own_audit'
WHERE setting_key = 'role.permissions.advocate';

UPDATE system_settings
SET setting_value = 'view_all,view_docs,upload_docs,manage_tasks_close,view_own_billing,scope_own,view_own_audit'
WHERE setting_key = 'role.permissions.clerk';

UPDATE system_settings
SET setting_value = 'view_all,view_docs,scope_own'
WHERE setting_key = 'role.permissions.readonly';

-- Apply new permissions to existing users by role so they take effect immediately
-- Admin: full set
UPDATE users
SET permissions = ARRAY['view_all','create_matters','edit_matters','delete_matters','view_docs','upload_docs','edit_docs','delete_docs','manage_tasks','manage_tasks_assign','manage_tasks_close','view_billing','view_own_billing','create_invoices','export_billing','export_data','manage_clients','delete_clients','manage_users','manage_roles','impersonate_user','system_settings','view_audit','view_own_audit','view_org_audit','scope_org','use_court_tracker','manage_filings']
WHERE role = 'admin' AND tenant_id = 'default';

-- Senior: tracker + filings + org audit
UPDATE users
SET permissions = ARRAY['view_all','create_matters','edit_matters','view_docs','upload_docs','edit_docs','manage_tasks','manage_tasks_assign','manage_tasks_close','view_billing','export_billing','manage_clients','export_data','scope_team','use_court_tracker','manage_filings','view_org_audit']
WHERE role = 'senior' AND tenant_id = 'default';

-- Advocate: tracker + filings + own audit
UPDATE users
SET permissions = ARRAY['view_all','create_matters','edit_matters','view_docs','upload_docs','edit_docs','manage_tasks_close','view_own_billing','scope_own','use_court_tracker','manage_filings','view_own_audit']
WHERE role = 'advocate' AND tenant_id = 'default';

-- Clerk: view + own audit (no tracker, no filings)
UPDATE users
SET permissions = ARRAY['view_all','view_docs','upload_docs','manage_tasks_close','view_own_billing','scope_own','view_own_audit']
WHERE role = 'clerk' AND tenant_id = 'default';

-- Read-only: view only
UPDATE users
SET permissions = ARRAY['view_all','view_docs','scope_own']
WHERE role = 'readonly' AND tenant_id = 'default';
