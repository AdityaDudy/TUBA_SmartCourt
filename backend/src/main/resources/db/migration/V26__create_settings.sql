-- V26: Create System Settings table
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(1000) NOT NULL
);

-- Seed initial default settings
INSERT INTO system_settings (setting_key, setting_value) VALUES
('firmName', 'TUBA Law Associates'),
('firmEmail', 'info@tubalaw.com'),
('firmPhone', '+91-11-4567-8900'),
('firmAddress', 'New Delhi, India'),
('currency', 'INR'),
('timezone', 'Asia/Kolkata'),
('dateFormat', 'DD/MM/YYYY'),
('hearingReminders', 'true'),
('taskReminders', 'true'),
('emailAlerts', 'true'),
('whatsappAlerts', 'false'),
('overdueAlerts', 'true'),
('mfaEnabled', 'true'),
('maxAttempts', '5'),
('sessionTimeout', '1 hr')
ON CONFLICT (setting_key) DO NOTHING;
