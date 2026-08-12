-- V12: Add priority column to matters table
ALTER TABLE matters ADD COLUMN IF NOT EXISTS priority VARCHAR(50) DEFAULT 'Medium';
