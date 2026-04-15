-- Remove cleanup functionality from integrations table
ALTER TABLE integrations DROP COLUMN IF EXISTS cleanup_enabled;
ALTER TABLE integrations DROP COLUMN IF EXISTS cleanup_age_value;
ALTER TABLE integrations DROP COLUMN IF EXISTS cleanup_age_unit;
