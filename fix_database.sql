-- Manual fix for removing cleanup columns
-- Run this in H2 Console: http://localhost:8085/h2-console
-- JDBC URL: jdbc:h2:file:./data/fsmonitor
-- User: sa
-- Password: password

ALTER TABLE integrations DROP COLUMN cleanup_enabled;
ALTER TABLE integrations DROP COLUMN cleanup_age_value;
ALTER TABLE integrations DROP COLUMN cleanup_age_unit;

-- Verify columns are gone
SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'INTEGRATIONS' 
AND COLUMN_NAME LIKE 'CLEANUP%';
