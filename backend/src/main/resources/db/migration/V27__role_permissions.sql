-- V27: Role Permissions Settings Defaults
INSERT INTO system_settings (setting_key, setting_value) VALUES
('role.permissions.admin', 'view_all,create_matters,edit_matters,delete_matters,view_docs,upload_docs,delete_docs,manage_tasks,manage_tasks_assign,manage_tasks_close,view_billing,view_own_billing,create_invoices,export_billing,export_data,manage_clients,delete_clients,manage_users,manage_roles,impersonate_user,system_settings,view_audit,view_own_audit,scope_org'),
('role.permissions.senior', 'view_all,create_matters,edit_matters,view_docs,upload_docs,manage_tasks,manage_tasks_assign,manage_tasks_close,view_billing,export_billing,manage_clients,export_data,scope_team'),
('role.permissions.advocate', 'view_all,create_matters,edit_matters,view_docs,upload_docs,manage_tasks_close,view_own_billing,scope_own'),
('role.permissions.clerk', 'view_all,view_docs,upload_docs,manage_tasks_close,view_own_billing,scope_own'),
('role.permissions.readonly', 'view_all,view_docs,scope_own')
ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value;
