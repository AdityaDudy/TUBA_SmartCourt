-- Migration V35: Add order_category column to case_orders table
ALTER TABLE case_orders ADD COLUMN IF NOT EXISTS order_category VARCHAR(20);
