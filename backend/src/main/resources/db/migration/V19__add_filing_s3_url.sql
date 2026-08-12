-- V19: Add s3_url column to filings table to support document attachments
ALTER TABLE filings ADD COLUMN s3_url VARCHAR(1000);
