-- Migration V34: Remove captcha_image_s3_url column from scrape_jobs
ALTER TABLE scrape_jobs DROP COLUMN IF EXISTS captcha_image_s3_url;
