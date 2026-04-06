-- Add check_interval_minutes column to log_configs table if it doesn't exist
-- and set default value for existing rows

ALTER TABLE log_configs ADD COLUMN IF NOT EXISTS check_interval_minutes INTEGER;

-- Update existing rows to have default value of 5 minutes
UPDATE log_configs SET check_interval_minutes = 5 WHERE check_interval_minutes IS NULL;
