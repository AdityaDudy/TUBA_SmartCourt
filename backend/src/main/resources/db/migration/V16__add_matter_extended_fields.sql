ALTER TABLE matters ADD COLUMN co_counsel VARCHAR(255);
ALTER TABLE matters ADD COLUMN opposing_counsel VARCHAR(255);
ALTER TABLE matters ADD COLUMN limitation_deadline DATE;
ALTER TABLE matters ADD COLUMN related_matter_id BIGINT;
