-- Add theme preference column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS theme VARCHAR(50) DEFAULT 'green:light' 
CHECK (theme LIKE '%:%');
