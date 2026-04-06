-- Add recursive column to log_configs table
-- Controls whether directory searches should be recursive or only top-level

ALTER TABLE log_configs ADD COLUMN IF NOT EXISTS recursive BOOLEAN DEFAULT TRUE;

-- Update existing rows to have recursive = true by default
UPDATE log_configs SET recursive = TRUE WHERE recursive IS NULL;
