-- V24: Seed audit logs
INSERT INTO audit_log (tenant_id, user_id, user_email, action, entity, entity_id, details, ip_address, risk, created_at) VALUES
('default', 1, 'amit@tubalaw.com', 'Login Success', 'Auth', '1', 'Logged in from 103.21.244.1 using Chrome (Windows 11)', '103.21.244.1', 'LOW', NOW() - INTERVAL '2 hours'),
('default', 1, 'amit@tubalaw.com', 'IP Rule Added', 'Security', '1', 'Added rule: 103.21.244.0/22 (Office LAN), blocked=false', '103.21.244.1', 'MEDIUM', NOW() - INTERVAL '1 hour'),
('default', 1, 'amit@tubalaw.com', 'IP Rule Added', 'Security', '2', 'Added rule: 49.248.8.100/32 (Sr. Adv. Home), blocked=false', '103.21.244.1', 'MEDIUM', NOW() - INTERVAL '45 minutes'),
('default', 1, 'amit@tubalaw.com', 'Security Settings Updated', 'Settings', NULL, 'Updated security parameters: MFA enabled=true, Max Attempts=5, Session Timeout=1 hr', '103.21.244.1', 'HIGH', NOW() - INTERVAL '30 minutes');
