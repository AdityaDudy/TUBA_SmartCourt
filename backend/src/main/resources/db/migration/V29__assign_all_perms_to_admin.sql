-- V29: Sync all permissions to admin users and admin role default
UPDATE users 
SET permissions = ARRAY['view_all','create_matters','edit_matters','delete_matters','view_docs','upload_docs','delete_docs','manage_tasks','manage_tasks_assign','manage_tasks_close','view_billing','view_own_billing','create_invoices','export_billing','export_data','manage_clients','delete_clients','manage_users','manage_roles','impersonate_user','system_settings','view_audit','view_own_audit','scope_org']
WHERE LOWER(role) = 'admin';

UPDATE system_settings
SET setting_value = 'view_all,create_matters,edit_matters,delete_matters,view_docs,upload_docs,delete_docs,manage_tasks,manage_tasks_assign,manage_tasks_close,view_billing,view_own_billing,create_invoices,export_billing,export_data,manage_clients,delete_clients,manage_users,manage_roles,impersonate_user,system_settings,view_audit,view_own_audit,scope_org'
WHERE setting_key = 'role.permissions.admin';
